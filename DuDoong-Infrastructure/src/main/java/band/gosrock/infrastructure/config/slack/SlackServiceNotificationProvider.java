package band.gosrock.infrastructure.config.slack;


import com.slack.api.model.block.LayoutBlock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackServiceNotificationProvider {

    private final SlackHelper slackHelper;

    @Value("${slack.webhook.service-alarm-channel}")
    private String CHANNEL_ID;

    public void sendNotification(List<LayoutBlock> layoutBlocks) {
        slackHelper.sendNotification(CHANNEL_ID, layoutBlocks);
    }
}
