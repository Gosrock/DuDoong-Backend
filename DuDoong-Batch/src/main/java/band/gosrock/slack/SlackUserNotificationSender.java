package band.gosrock.slack;

import static com.slack.api.model.block.Blocks.divider;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;

import band.gosrock.infrastructure.config.slack.SlackServiceNotificationProvider;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackUserNotificationSender {
    private final SlackServiceNotificationProvider slackProvider;

    public void execute(LocalDate date, Long todayUserCount, Long yesterdayCount) {

        List<LayoutBlock> layoutBlocks = new ArrayList<>();
        layoutBlocks.add(
                Blocks.header(
                        headerBlockBuilder -> headerBlockBuilder.text(plainText("유저 관련 일일 통계"))));
        layoutBlocks.add(divider());
        MarkdownTextObject markdownDate =
                MarkdownTextObject.builder().text("* 실행 일:*\n" + date).build();
        MarkdownTextObject markdownTotalUser =
                MarkdownTextObject.builder().text("* 당일 총 유저 수:*\n" + todayUserCount).build();
        layoutBlocks.add(
                section(section -> section.fields(List.of(markdownDate, markdownTotalUser))));

        layoutBlocks.add(divider());

        MarkdownTextObject markdownYesterdayCount =
                MarkdownTextObject.builder().text("* 전일 총 유저 수 :*\n" + yesterdayCount).build();
        MarkdownTextObject markdownUserIncrease =
                MarkdownTextObject.builder()
                        .text("* 유저 증감 :*\n" + (todayUserCount - yesterdayCount))
                        .build();
        layoutBlocks.add(
                section(
                        section ->
                                section.fields(
                                        List.of(markdownYesterdayCount, markdownUserIncrease))));

        slackProvider.sendNotification(layoutBlocks);
    }
}
