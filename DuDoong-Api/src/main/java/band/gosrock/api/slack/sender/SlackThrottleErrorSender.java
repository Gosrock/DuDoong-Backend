package band.gosrock.api.slack.sender;

import static com.slack.api.model.block.Blocks.divider;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;

import band.gosrock.infrastructure.config.slack.SlackErrorNotificationProvider;
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
public class SlackThrottleErrorSender {
    private final ObjectMapper objectMapper;

    private final SlackErrorNotificationProvider slackProvider;

    public void execute(ContentCachingRequestWrapper cachingRequest, Long userId)
            throws IOException {
        final String url = cachingRequest.getRequestURL().toString();
        final String method = cachingRequest.getMethod();
        final String body =
                objectMapper.readTree(cachingRequest.getContentAsByteArray()).toString();
        final String errorUserIP = cachingRequest.getRemoteAddr();

        List<LayoutBlock> layoutBlocks = new ArrayList<>();
        layoutBlocks.add(
                Blocks.header(
                        headerBlockBuilder ->
                                headerBlockBuilder.text(plainText("Rate Limit Error"))));
        layoutBlocks.add(divider());

        MarkdownTextObject errorUserIdMarkdown =
                MarkdownTextObject.builder().text("* User Id :*\n" + userId).build();
        MarkdownTextObject errorUserIpMarkdown =
                MarkdownTextObject.builder().text("* User IP :*\n" + errorUserIP).build();
        layoutBlocks.add(
                section(
                        section ->
                                section.fields(List.of(errorUserIdMarkdown, errorUserIpMarkdown))));

        MarkdownTextObject methodMarkdown =
                MarkdownTextObject.builder()
                        .text("* Request Addr :*\n" + method + " : " + url)
                        .build();
        MarkdownTextObject bodyMarkdown =
                MarkdownTextObject.builder().text("* Request Body :*\n" + body).build();
        List<TextObject> fields = List.of(methodMarkdown, bodyMarkdown);
        layoutBlocks.add(section(section -> section.fields(fields)));

        layoutBlocks.add(divider());

        slackProvider.sendNotification(layoutBlocks);
    }
}
