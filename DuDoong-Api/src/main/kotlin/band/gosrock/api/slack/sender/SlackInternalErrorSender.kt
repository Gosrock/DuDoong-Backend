package band.gosrock.api.slack.sender

import com.fasterxml.jackson.databind.ObjectMapper
import com.slack.api.model.block.Blocks
import com.slack.api.model.block.Blocks.divider
import com.slack.api.model.block.Blocks.section
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.composition.BlockCompositions.plainText
import com.slack.api.model.block.composition.MarkdownTextObject
import band.gosrock.api.config.SensitiveBodyMasker
import band.gosrock.infrastructure.config.slack.SlackErrorNotificationProvider
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.util.ContentCachingRequestWrapper
import java.io.IOException

private val log = LoggerFactory.getLogger(SlackInternalErrorSender::class.java)

@Component
class SlackInternalErrorSender(
    private val objectMapper: ObjectMapper,
    private val slackProvider: SlackErrorNotificationProvider,
) {
    @Throws(IOException::class)
    fun execute(cachingRequest: ContentCachingRequestWrapper, e: Exception, userId: Long) {
        // 선물 링크 토큰은 URL 에서 가린다 (#719)
        val url = SensitiveBodyMasker.maskPath(cachingRequest.requestURL.toString())
        val method = cachingRequest.method
        // 민감 키(계좌·입금자명·연락처) 값은 마스킹해서 보낸다
        val body = SensitiveBodyMasker.mask(objectMapper.readTree(cachingRequest.contentAsByteArray).toString())
        val errorMessage = e.message
        val errorStack = slackProvider.getErrorStack(e)
        val errorUserIP = cachingRequest.remoteAddr

        val layoutBlocks = mutableListOf<LayoutBlock>()
        layoutBlocks.add(
            Blocks.header { it.text(plainText("Error Detection")) }
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

        val errorNameMarkdown = MarkdownTextObject.builder().text("* Message :*\n$errorMessage").build()
        val errorStackMarkdown = MarkdownTextObject.builder().text("* Stack Trace :*\n$errorStack").build()
        layoutBlocks.add(section { it.fields(listOf(errorNameMarkdown, errorStackMarkdown)) })

        slackProvider.sendNotification(layoutBlocks)
    }
}
