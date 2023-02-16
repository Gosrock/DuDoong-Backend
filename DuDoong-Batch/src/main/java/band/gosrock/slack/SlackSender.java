package band.gosrock.slack;

import static com.slack.api.model.block.Blocks.divider;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;

import band.gosrock.infrastructure.config.slack.SlackErrorNotificationProvider;
import band.gosrock.infrastructure.config.slack.SlackServiceNotificationProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.TextObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackSender {
    private final SlackServiceNotificationProvider slackProvider;

    public void execute(Long userCount){

        List<LayoutBlock> layoutBlocks = new ArrayList<>();
        layoutBlocks.add(
                Blocks.header(
                        headerBlockBuilder ->
                                headerBlockBuilder.text(plainText("유저 관련 일일 통계"))));
        layoutBlocks.add(divider());

        MarkdownTextObject errorUserIdMarkdown =
                MarkdownTextObject.builder().text("* 총 유저 수:*\n" + userCount).build();
        layoutBlocks.add(
                section(
                        section ->
                                section.fields(List.of(errorUserIdMarkdown))));

        slackProvider.sendNotification(layoutBlocks);
    }
}
