package band.gosrock.api.config.slack;

import static com.slack.api.model.block.Blocks.divider;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.TextObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackApiProvider {
    private final MethodsClient methodsClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.profiles.active}")
    private String ACTIVE_PROFILE;

    @Value("${slack.webhook.id}")
    private String CHANNEL_ID;

    private final int MAX_LEN = 500;

    @Async
    public void sendError(ContentCachingRequestWrapper cachingRequest, Exception e, Long userId)
            throws IOException {
        if (Arrays.asList("staging", "prod").contains(ACTIVE_PROFILE)) {
            executeSendError(cachingRequest, e, userId);
        }
    }

    private void executeSendError(
            ContentCachingRequestWrapper cachingRequest, Exception e, Long userId)
            throws IOException {
        final String url = cachingRequest.getRequestURL().toString();
        final String method = cachingRequest.getMethod();
        final String body =
                objectMapper.readTree(cachingRequest.getContentAsByteArray()).toString();
        final String exceptionAsString = Arrays.toString(e.getStackTrace());
        final int cutLength = Math.min(exceptionAsString.length(), MAX_LEN);

        final String errorMessage = e.getMessage();
        final String errorStack = exceptionAsString.substring(0, cutLength);
        final String errorUserIP = cachingRequest.getRemoteAddr();

        List<LayoutBlock> layoutBlocks = new ArrayList<>();
        layoutBlocks.add(
                Blocks.header(
                        headerBlockBuilder ->
                                headerBlockBuilder.text(plainText("Error Detection"))));
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

        MarkdownTextObject errorNameMarkdown =
                MarkdownTextObject.builder().text("* Message :*\n" + errorMessage).build();
        MarkdownTextObject errorStackMarkdown =
                MarkdownTextObject.builder().text("* Stack Trace :*\n" + errorStack).build();
        layoutBlocks.add(
                section(section -> section.fields(List.of(errorNameMarkdown, errorStackMarkdown))));

        ChatPostMessageRequest chatPostMessageRequest =
                ChatPostMessageRequest.builder()
                        .channel(CHANNEL_ID)
                        .text("")
                        .blocks(layoutBlocks)
                        .build();
        try {
            methodsClient.chatPostMessage(chatPostMessageRequest);
        } catch (SlackApiException slackApiException) {
            log.error(slackApiException.toString());
        }
    }
}
