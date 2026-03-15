package band.gosrock.helper.slack

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.infrastructure.config.slack.SlackServiceNotificationProvider
import com.slack.api.model.block.Blocks
import com.slack.api.model.block.Blocks.divider
import com.slack.api.model.block.Blocks.section
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.composition.BlockCompositions.plainText
import com.slack.api.model.block.composition.MarkdownTextObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SlackEventExpirationSender(
    private val slackProvider: SlackServiceNotificationProvider,
) {
    private val log = LoggerFactory.getLogger(SlackEventExpirationSender::class.java)

    fun execute(time: LocalDateTime, events: List<Event>) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val layoutBlocks = mutableListOf<LayoutBlock>()

        layoutBlocks.add(
            Blocks.header { it.text(plainText("공연 자동 만료 알림")) }
        )
        layoutBlocks.add(divider())

        val markdownDateTime = MarkdownTextObject.builder().text("* 기준 시간:*\n" + time.format(formatter)).build()
        val markdownTotalEvents = MarkdownTextObject.builder().text("* 만료 공연 수:*\n" + events.size).build()
        layoutBlocks.add(section { it.fields(listOf(markdownDateTime, markdownTotalEvents)) })
        layoutBlocks.add(divider())

        val markdownEventIdTitle = MarkdownTextObject.builder().text("* 공연 ID:*\n").build()
        val markdownEventNameTitle = MarkdownTextObject.builder().text("* 공연 이름:*\n").build()
        layoutBlocks.add(section { it.fields(listOf(markdownEventIdTitle, markdownEventNameTitle)) })

        events.forEach { event ->
            val markdownEventId = MarkdownTextObject.builder().text(event.id.toString()).build()
            val markdownEventName = MarkdownTextObject.builder().text(event.eventBasic!!.name!!).build()
            layoutBlocks.add(section { it.fields(listOf(markdownEventId, markdownEventName)) })
        }

        slackProvider.sendNotification(layoutBlocks)
    }
}
