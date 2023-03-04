package band.gosrock.slack;

import static com.slack.api.model.block.Blocks.divider;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;

import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.infrastructure.config.slack.SlackServiceNotificationProvider;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackEventExpirationSender {
    private final SlackServiceNotificationProvider slackProvider;

    public void execute(LocalDateTime time, List<Event> events) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        List<LayoutBlock> layoutBlocks = new ArrayList<>();

        layoutBlocks.add(
                Blocks.header(
                        headerBlockBuilder -> headerBlockBuilder.text(plainText("공연 자동 만료 알림"))));
        layoutBlocks.add(divider());
        MarkdownTextObject markdownDateTime =
                MarkdownTextObject.builder().text("* 기준 시간:*\n" + time.format(formatter)).build();
        MarkdownTextObject markdownTotalEvents =
                MarkdownTextObject.builder().text("* 만료 공연 수:*\n" + events.size()).build();
        layoutBlocks.add(
                section(section -> section.fields(List.of(markdownDateTime, markdownTotalEvents))));
        layoutBlocks.add(divider());

        MarkdownTextObject markdownEventIdTitle =
                MarkdownTextObject.builder().text("* 공연 ID:*\n").build();
        MarkdownTextObject markdownEventNameTitle =
                MarkdownTextObject.builder().text("* 공연 이름:*\n").build();
        layoutBlocks.add(
                section(
                        section ->
                                section.fields(
                                        List.of(markdownEventIdTitle, markdownEventNameTitle))));

        events.forEach(
                event -> {
                    MarkdownTextObject markdownEventId =
                            MarkdownTextObject.builder().text(event.getId().toString()).build();
                    MarkdownTextObject markdownEventName =
                            MarkdownTextObject.builder()
                                    .text(event.getEventBasic().getName())
                                    .build();
                    layoutBlocks.add(
                            section(
                                    section ->
                                            section.fields(
                                                    List.of(markdownEventId, markdownEventName))));
                });

        slackProvider.sendNotification(layoutBlocks);
    }
}
