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
public class SlackServiceNotificationProvider {

    private final SlackHelper slackHelper;
    @Value("${slack.webhook.service-alarm-channel}")
    private String CHANNEL_ID;


    public void sendNotification(List<LayoutBlock> layoutBlocks) {
        slackHelper.sendNotification(CHANNEL_ID,layoutBlocks);
    }
}
