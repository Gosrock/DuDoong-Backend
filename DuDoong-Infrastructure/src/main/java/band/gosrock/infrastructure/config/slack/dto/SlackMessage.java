package band.gosrock.infrastructure.config.slack.dto;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SlackMessage {
    private String text;
    private String username;
    private String icon_url;
}
