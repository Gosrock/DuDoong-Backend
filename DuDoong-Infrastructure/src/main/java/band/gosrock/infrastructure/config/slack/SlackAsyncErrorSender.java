package band.gosrock.infrastructure.config.slack;

import static com.slack.api.model.block.Blocks.divider;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;

import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class SlackAsyncErrorSender {
    private final SlackErrorNotificationProvider slackProvider;

    public void execute(String name, Throwable throwable, Object[] params) {
        List<LayoutBlock> layoutBlocks = new ArrayList<>();
        layoutBlocks.add(
                Blocks.header(
                        headerBlockBuilder -> headerBlockBuilder.text(plainText("비동기 에러 알림"))));
        layoutBlocks.add(divider());

        MarkdownTextObject errorUserIdMarkdown =
                MarkdownTextObject.builder().text("* 메소드 이름 :*\n" + name).build();
        MarkdownTextObject errorUserIpMarkdown =
                MarkdownTextObject.builder()
                        .text("* 요청 파라미터 :*\n" + getParamsToString(params))
                        .build();
        layoutBlocks.add(
                section(
                        section ->
                                section.fields(List.of(errorUserIdMarkdown, errorUserIpMarkdown))));

        layoutBlocks.add(divider());
        String errorStack = slackProvider.getErrorStack(throwable);
        String message = throwable.toString();
        MarkdownTextObject errorNameMarkdown =
                MarkdownTextObject.builder().text("* Message :*\n" + message).build();
        MarkdownTextObject errorStackMarkdown =
                MarkdownTextObject.builder().text("* Stack Trace :*\n" + errorStack).build();
        layoutBlocks.add(
                section(section -> section.fields(List.of(errorNameMarkdown, errorStackMarkdown))));
        slackProvider.sendNotification(layoutBlocks);
    }

    private String getParamsToString(Object[] params) {
        StringBuilder paramToString = new StringBuilder();
        for (Object param : params) {
            paramToString.append(param.toString());
        }
        return paramToString.toString();
    }
}
