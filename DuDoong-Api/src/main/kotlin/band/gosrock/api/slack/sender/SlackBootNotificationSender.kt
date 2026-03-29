package band.gosrock.api.slack.sender

import band.gosrock.common.helper.SpringEnvironmentHelper
import band.gosrock.infrastructure.config.slack.SlackServiceNotificationProvider
import com.slack.api.model.block.Blocks
import com.slack.api.model.block.Blocks.divider
import com.slack.api.model.block.Blocks.section
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.composition.BlockCompositions.plainText
import com.slack.api.model.block.composition.MarkdownTextObject
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class SlackBootNotificationSender(
    private val slackServiceNotificationProvider: SlackServiceNotificationProvider,
    private val springEnvironmentHelper: SpringEnvironmentHelper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        try {
            val layoutBlocks = mutableListOf<LayoutBlock>()
            layoutBlocks.add(Blocks.header { it.text(plainText("Application Started")) })
            layoutBlocks.add(divider())

            val profile = resolveProfile()
            val hostname = runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("unknown")
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

            val profileMarkdown = MarkdownTextObject.builder().text("* Profile :*\n`$profile`").build()
            val hostMarkdown = MarkdownTextObject.builder().text("* Host :*\n`$hostname`").build()
            layoutBlocks.add(section { it.fields(listOf(profileMarkdown, hostMarkdown)) })

            val timeMarkdown = MarkdownTextObject.builder().text("* Started At :*\n$timestamp").build()
            layoutBlocks.add(section { it.fields(listOf(timeMarkdown)) })

            slackServiceNotificationProvider.sendNotification(layoutBlocks)
            log.info("Boot notification sent to Slack (profile={})", profile)
        } catch (e: Exception) {
            log.warn("Failed to send boot notification to Slack", e)
        }
    }

    private fun resolveProfile(): String = when {
        springEnvironmentHelper.isProdProfile() -> "prod"
        springEnvironmentHelper.isStagingProfile() -> "staging"
        springEnvironmentHelper.isDevProfile() -> "dev"
        else -> "local"
    }
}
