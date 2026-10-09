package band.gosrock.api.slack.sender

import band.gosrock.api.config.SensitiveBodyMasker
import com.fasterxml.jackson.databind.ObjectMapper
import com.slack.api.model.block.Blocks
import com.slack.api.model.block.Blocks.divider
import com.slack.api.model.block.Blocks.section
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.composition.BlockCompositions.plainText
import com.slack.api.model.block.composition.MarkdownTextObject
import band.gosrock.infrastructure.config.slack.SlackErrorNotificationProvider
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.task.TaskRejectedException
import org.springframework.stereotype.Component
import org.springframework.web.util.ContentCachingRequestWrapper
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

private val log = LoggerFactory.getLogger(SlackThrottleErrorSender::class.java)

@Component
class SlackThrottleErrorSender(
    private val objectMapper: ObjectMapper,
    private val slackProvider: SlackErrorNotificationProvider,
) {
    /** 알림 키(유저 id 또는 IP)별 마지막 발송 시각(ms) */
    private val lastSentAt = ConcurrentHashMap<String, Long>()

    /**
     * 요청 제한 알림. 같은 키(로그인 유저는 유저 id, 비로그인은 IP)는 [INTERVAL_MILLIS] 에 한 번만 보낸다 (#764).
     * 실제 전송은 비동기 풀에서 하고, 풀이 가득 차 거절되면 로그만 남긴다 — 요청 스레드는 기다리거나 실패하지 않는다
     */
    @Throws(IOException::class)
    fun execute(cachingRequest: ContentCachingRequestWrapper, userId: Long, nowMillis: Long = System.currentTimeMillis()) {
        val key = if (userId == 0L) "ip:${cachingRequest.remoteAddr}" else "user:$userId"
        if (!tryAcquire(key, nowMillis)) return

        // 선물 링크 토큰(URL)·민감 키(본문)는 가려서 보낸다 (#719 리뷰 — 500 알림과 같은 규칙)
        val url = SensitiveBodyMasker.maskPath(cachingRequest.requestURL.toString())
        val method = cachingRequest.method
        val body = SensitiveBodyMasker.mask(objectMapper.readTree(cachingRequest.contentAsByteArray).toString())
        val errorUserIP = cachingRequest.remoteAddr

        val layoutBlocks = mutableListOf<LayoutBlock>()
        layoutBlocks.add(
            Blocks.header { it.text(plainText("Rate Limit Error")) }
        )
        layoutBlocks.add(divider())

        val traceId = MDC.get("traceId") ?: "no-trace"

        val errorUserIdMarkdown = MarkdownTextObject.builder().text("* User Id :*\n$userId").build()
        val errorUserIpMarkdown = MarkdownTextObject.builder().text("* User IP :*\n$errorUserIP").build()
        layoutBlocks.add(section { it.fields(listOf(errorUserIdMarkdown, errorUserIpMarkdown)) })

        val traceIdMarkdown = MarkdownTextObject.builder().text("* Trace ID :*\n`$traceId`").build()
        layoutBlocks.add(section { it.fields(listOf(traceIdMarkdown)) })

        val methodMarkdown = MarkdownTextObject.builder().text("* Request Addr :*\n$method : $url").build()
        val bodyMarkdown = MarkdownTextObject.builder().text("* Request Body :*\n$body").build()
        layoutBlocks.add(section { it.fields(listOf(methodMarkdown, bodyMarkdown)) })

        layoutBlocks.add(divider())

        try {
            slackProvider.sendNotification(layoutBlocks)
        } catch (e: TaskRejectedException) {
            log.warn("rate limit Slack 알림 생략 (비동기 풀 포화) key={}", key)
        }
    }

    @Volatile
    private var lastCleanupAt = 0L

    /**
     * 키별로 [INTERVAL_MILLIS] 에 한 번만 true.
     * 지난 키 정리는 [INTERVAL_MILLIS] 에 한 번만 한다(요청마다 전체를 훑지 않는다). 그 사이 키가 [MAX_KEYS] 를 넘으면 새 키 알림은 생략한다(메모리 상한)
     */
    internal fun tryAcquire(key: String, nowMillis: Long): Boolean {
        if (nowMillis - lastCleanupAt >= INTERVAL_MILLIS) {
            lastCleanupAt = nowMillis
            lastSentAt.entries.removeIf { nowMillis - it.value >= INTERVAL_MILLIS }
        }
        if (lastSentAt.size >= MAX_KEYS && !lastSentAt.containsKey(key)) return false
        var acquired = false
        lastSentAt.compute(key) { _, last ->
            if (last == null || nowMillis - last >= INTERVAL_MILLIS) {
                acquired = true
                nowMillis
            } else {
                last
            }
        }
        return acquired
    }

    internal fun trackedKeyCount(): Int = lastSentAt.size

    companion object {
        const val INTERVAL_MILLIS = 60_000L
        internal const val MAX_KEYS = 10_000
    }
}
