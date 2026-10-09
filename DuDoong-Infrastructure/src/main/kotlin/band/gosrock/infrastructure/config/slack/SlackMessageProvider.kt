package band.gosrock.infrastructure.config.slack

import com.slack.api.Slack
import com.slack.api.util.http.SlackHttpClient
import com.slack.api.webhook.Payload
import okhttp3.OkHttpClient
import org.apache.commons.codec.binary.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.IOException
import java.net.UnknownHostException

@Service
class SlackMessageProvider(
    @Value("\${slack.webhook.username}") private val username: String,
    @Value("\${slack.webhook.icon-url}") private val iconUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 호스트 웹훅 전송용. 리다이렉트는 따라가지 않는다 (#764) — 허용한 Slack 주소 밖으로 요청이 나가지 않게 */
    private val slack: Slack = Slack.getInstance(
        SlackHttpClient(OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build()),
    )

    /** 이벤트 핸들러 자체에서 비동기로 실행하기 때문에 @Async 어노테이션 지움 */
    fun sendMessage(url: String?, text: String) {
        if (url == null) return
        // 형식 검증 전에 저장된 URL 은 보내지 않는다 (#764)
        if (!isSlackWebhookUrl(url)) {
            log.warn("허용되지 않은 슬랙 웹훅 URL 이라 알림을 건너뜀")
            return
        }
        try {
            doSend(url, text)
        } catch (e: Exception) {
            // ignored
        }
    }

    /** 호스트가 존재하는 지 확인하기 위해 동기로 처리 */
    @Throws(UnknownHostException::class)
    fun register(url: String) {
        if (!isSlackWebhookUrl(url)) throw UnknownHostException("올바른 슬랙 URL이 아닙니다.")
        val text = "두둥 슬랙 알림이 성공적으로 등록되었습니다!"
        doSend(url, text)
    }

    @Throws(UnknownHostException::class)
    private fun doSend(url: String, text: String) {
        val payload = Payload.builder().text(text).username(username).iconUrl(iconUrl).build()
        try {
            val responseBody = slack.send(url, payload).body
            if (!StringUtils.equals(responseBody, "ok")) {
                throw UnknownHostException("올바른 슬랙 URL이 아닙니다.")
            }
        } catch (e: UnknownHostException) {
            throw e
        } catch (e: IOException) {
            log.error(e.message, e)
            throw RuntimeException(e)
        }
    }

    companion object {
        /** Slack Incoming Webhook 주소 형식. 요청 DTO 검증(@Pattern)과 같은 규칙 */
        const val SLACK_WEBHOOK_URL_PATTERN = "^https://hooks\\.slack\\.com/services/[A-Za-z0-9/_-]+$"
        private val SLACK_WEBHOOK_URL = Regex(SLACK_WEBHOOK_URL_PATTERN)

        fun isSlackWebhookUrl(url: String): Boolean = SLACK_WEBHOOK_URL.matches(url)
    }
}
