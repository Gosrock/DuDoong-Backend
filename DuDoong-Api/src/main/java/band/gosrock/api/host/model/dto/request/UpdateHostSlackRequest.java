package band.gosrock.api.host.model.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

/** 호스트 슬랙 알람 URL 수정 요청 DTO */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHostSlackRequest {
    @Schema(defaultValue = "https://slack.dd.com", description = "슬랙 웹훅 URL")
    @NotBlank(message = "올바른 슬랙 URL 을 입력해주세요")
    @URL(message = "올바른 슬랙 URL 을 입력해주세요")
    private String slackUrl;
}
