package band.gosrock.infrastructure.config.slack;


import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.block.LayoutBlock;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackServerNotificationProvider {

    private final Environment env;

    private final List<String> sendAlarmProfiles = List.of("staging", "prod");
    private final int MAX_LEN = 500;

    @Value("${slack.webhook.id}")
    private String CHANNEL_ID;

    private final MethodsClient methodsClient;

    private Boolean isNeedToNotificationProfile() {
        String[] activeProfiles = env.getActiveProfiles();
        List<String> currentProfile = Arrays.stream(activeProfiles).toList();
        return CollectionUtils.containsAny(sendAlarmProfiles, currentProfile);
    }

    public String getErrorStack(Throwable throwable) {
        String exceptionAsString = Arrays.toString(throwable.getStackTrace());
        int cutLength = Math.min(exceptionAsString.length(), MAX_LEN);
        return exceptionAsString.substring(0, cutLength);
    }

    @Async
    public void sendNotification(List<LayoutBlock> layoutBlocks) {
        if (!isNeedToNotificationProfile()) {
            return;
        }
        ChatPostMessageRequest chatPostMessageRequest =
                ChatPostMessageRequest.builder()
                        .channel(CHANNEL_ID)
                        .text("")
                        .blocks(layoutBlocks)
                        .build();
        try {
            methodsClient.chatPostMessage(chatPostMessageRequest);
        } catch (SlackApiException | IOException slackApiException) {
            log.error(slackApiException.toString());
        }
    }
}
