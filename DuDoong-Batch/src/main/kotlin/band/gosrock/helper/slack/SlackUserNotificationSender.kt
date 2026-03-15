package band.gosrock.helper.slack

import band.gosrock.infrastructure.config.slack.SlackServiceNotificationProvider
import com.slack.api.model.block.Blocks
import com.slack.api.model.block.Blocks.divider
import com.slack.api.model.block.Blocks.section
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.composition.BlockCompositions.plainText
import com.slack.api.model.block.composition.MarkdownTextObject
import java.time.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SlackUserNotificationSender(
    private val slackProvider: SlackServiceNotificationProvider,
) {
    private val log = LoggerFactory.getLogger(SlackUserNotificationSender::class.java)

    fun execute(date: LocalDate, todayUserCount: Long, yesterdayCount: Long) {
        val layoutBlocks = mutableListOf<LayoutBlock>()
        layoutBlocks.add(
            Blocks.header { it.text(plainText("유저 관련 일일 통계")) }
        )
        layoutBlocks.add(divider())

        val markdownDate = MarkdownTextObject.builder().text("* 실행 일:*\n$date").build()
        val markdownTotalUser = MarkdownTextObject.builder().text("* 당일 총 유저 수:*\n$todayUserCount").build()
        layoutBlocks.add(section { it.fields(listOf(markdownDate, markdownTotalUser)) })

        layoutBlocks.add(divider())

        val markdownYesterdayCount = MarkdownTextObject.builder().text("* 전일 총 유저 수 :*\n$yesterdayCount").build()
        val markdownUserIncrease = MarkdownTextObject.builder()
            .text("* 유저 증감 :*\n${todayUserCount - yesterdayCount}")
            .build()
        layoutBlocks.add(section { it.fields(listOf(markdownYesterdayCount, markdownUserIncrease)) })

        slackProvider.sendNotification(layoutBlocks)
    }
}
