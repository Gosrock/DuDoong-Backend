package band.gosrock.infrastructure.config.slack

import com.slack.api.model.block.LayoutBlock
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class SlackErrorNotificationProvider(
    private val slackHelper: SlackHelper,
    @Value("\${slack.webhook.id}") private val channelId: String,
) {
    private val maxLen = 500

    fun getErrorStack(throwable: Throwable): String {
        val exceptionAsString = throwable.stackTrace.contentToString()
        val cutLength = minOf(exceptionAsString.length, maxLen)
        return exceptionAsString.substring(0, cutLength)
    }

    @Async
    fun sendNotification(layoutBlocks: List<LayoutBlock>) {
        slackHelper.sendNotification(channelId, layoutBlocks)
    }
}
