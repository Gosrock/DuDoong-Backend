package band.gosrock.infrastructure.config.slack

import com.slack.api.model.block.LayoutBlock
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SlackServiceNotificationProvider(
    private val slackHelper: SlackHelper,
    @Value("\${slack.webhook.service-alarm-channel}") private val channelId: String,
) {
    fun sendNotification(layoutBlocks: List<LayoutBlock>) {
        slackHelper.sendNotification(channelId, layoutBlocks)
    }
}
