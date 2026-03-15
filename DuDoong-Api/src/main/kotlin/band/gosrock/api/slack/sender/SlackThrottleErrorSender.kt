package band.gosrock.api.slack.sender

import com.fasterxml.jackson.databind.ObjectMapper
import com.slack.api.model.block.Blocks
import com.slack.api.model.block.Blocks.divider
import com.slack.api.model.block.Blocks.section
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.composition.BlockCompositions.plainText
import com.slack.api.model.block.composition.MarkdownTextObject
import band.gosrock.infrastructure.config.slack.SlackErrorNotificationProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.util.ContentCachingRequestWrapper
import java.io.IOException

private val log = LoggerFactory.getLogger(SlackThrottleErrorSender::class.java)

@Component
class SlackThrottleErrorSender(
    private val objectMapper: ObjectMapper,
    private val slackProvider: SlackErrorNotificationProvider,
) {
    @Throws(IOException::class)
    fun execute(cachingRequest: ContentCachingRequestWrapper, userId: Long) {
        val url = cachingRequest.requestURL.toString()
        val method = cachingRequest.method
        val body = objectMapper.readTree(cachingRequest.contentAsByteArray).toString()
        val errorUserIP = cachingRequest.remoteAddr

        val layoutBlocks = mutableListOf<LayoutBlock>()
        layoutBlocks.add(
            Blocks.header { it.text(plainText("Rate Limit Error")) }
        )
        layoutBlocks.add(divider())

        val errorUserIdMarkdown = MarkdownTextObject.builder().text("* User Id :*\n$userId").build()
        val errorUserIpMarkdown = MarkdownTextObject.builder().text("* User IP :*\n$errorUserIP").build()
        layoutBlocks.add(section { it.fields(listOf(errorUserIdMarkdown, errorUserIpMarkdown)) })

        val methodMarkdown = MarkdownTextObject.builder().text("* Request Addr :*\n$method : $url").build()
        val bodyMarkdown = MarkdownTextObject.builder().text("* Request Body :*\n$body").build()
        layoutBlocks.add(section { it.fields(listOf(methodMarkdown, bodyMarkdown)) })

        layoutBlocks.add(divider())

        slackProvider.sendNotification(layoutBlocks)
    }
}
