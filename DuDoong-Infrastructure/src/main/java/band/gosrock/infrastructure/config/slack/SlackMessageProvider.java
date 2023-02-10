package band.gosrock.infrastructure.config.slack;


import com.slack.api.Slack;
import com.slack.api.webhook.Payload;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlackMessageProvider {

    @Value("${slack.webhook.username}")
    private String username;

    @Value("${slack.webhook.icon-url}")
    private String iconUrl;

    @Async
    public void sendMessage(String url, String text) {
        Slack slack = Slack.getInstance();
        Payload payload = Payload.builder().text(text).username(username).iconUrl(iconUrl).build();

        try {
            slack.send(url, payload);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
