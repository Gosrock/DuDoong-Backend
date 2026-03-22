package band.gosrock.infrastructure.config.slack

import com.slack.api.model.block.Blocks
import com.slack.api.model.block.Blocks.divider
import com.slack.api.model.block.Blocks.section
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.composition.BlockCompositions.plainText
import com.slack.api.model.block.composition.MarkdownTextObject
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
class SlackAsyncErrorSender(
    private val slackProvider: SlackErrorNotificationProvider,
) {
    fun execute(name: String, throwable: Throwable, params: Array<Any>) {
        val traceId = MDC.get("traceId") ?: "no-trace"
        val layoutBlocks = mutableListOf<LayoutBlock>()
        layoutBlocks.add(Blocks.header { it.text(plainText("비동기 에러 알림")) })
        layoutBlocks.add(divider())

        val traceIdMarkdown = MarkdownTextObject.builder().text("* Trace ID :*\n`$traceId`").build()
        val errorUserIdMarkdown = MarkdownTextObject.builder().text("* 메소드 이름 :*\n$name").build()
        layoutBlocks.add(section { it.fields(listOf(traceIdMarkdown, errorUserIdMarkdown)) })

        val errorUserIpMarkdown = MarkdownTextObject.builder()
            .text("* 요청 파라미터 :*\n${getParamsToString(params)}")
            .build()
        layoutBlocks.add(section { it.fields(listOf(errorUserIpMarkdown)) })

        layoutBlocks.add(divider())
        val errorStack = slackProvider.getErrorStack(throwable)
        val message = throwable.toString()
        val errorNameMarkdown = MarkdownTextObject.builder().text("* Message :*\n$message").build()
        val errorStackMarkdown = MarkdownTextObject.builder().text("* Stack Trace :*\n$errorStack").build()
        layoutBlocks.add(section { it.fields(listOf(errorNameMarkdown, errorStackMarkdown)) })

        slackProvider.sendNotification(layoutBlocks)
    }

    private fun getParamsToString(params: Array<Any>): String =
        params.joinToString("") { it.toString() }
}
