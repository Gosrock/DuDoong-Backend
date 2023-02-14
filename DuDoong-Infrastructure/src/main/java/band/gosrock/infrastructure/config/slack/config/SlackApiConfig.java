package band.gosrock.infrastructure.config.slack.config;


import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackApiConfig {

    @Value("${slack.webhook.token}")
    private String token;

    @Bean
    public MethodsClient getClient() {
        Slack slackClient = Slack.getInstance();
        return slackClient.methods(token);
    }
}
