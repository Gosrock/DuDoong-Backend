package band.gosrock.infrastructure.config.slack;


import lombok.RequiredArgsConstructor;
import net.gpedro.integrations.slack.SlackApi;
import net.gpedro.integrations.slack.SlackMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SlackMessageProvider {

    @Value("${slack.webhook.username}")
    private String username;

    @Value("${slack.webhook.icon-url}")
    private String iconUrl;

    @Async
    public void sendMessage(String url, String text) {
        final SlackApi slackApi = new SlackApi(url);
        final SlackMessage slackMessage = new SlackMessage();

        slackMessage.setText(text);
        slackMessage.setUsername(username);
        slackMessage.setIcon(iconUrl);
        slackApi.call(slackMessage);
    }
}
