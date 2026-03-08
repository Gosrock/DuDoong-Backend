package band.gosrock.infrastructure.config.slack

import band.gosrock.common.helper.SpringEnvironmentHelper
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.SlackApiException
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.model.block.LayoutBlock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class SlackHelper(
    private val springEnvironmentHelper: SpringEnvironmentHelper,
    private val methodsClient: MethodsClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendNotification(channelId: String, layoutBlocks: List<LayoutBlock>) {
        if (!springEnvironmentHelper.isProdAndStagingProfile()) return
        val request = ChatPostMessageRequest.builder()
            .channel(channelId)
            .text("")
            .blocks(layoutBlocks)
            .build()
        try {
            methodsClient.chatPostMessage(request)
        } catch (e: SlackApiException) {
            log.error(e.toString())
        } catch (e: IOException) {
            log.error(e.toString())
        }
    }
}
