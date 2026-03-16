package band.gosrock.api.host.model.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.URL

/** 호스트 슬랙 알람 URL 수정 요청 DTO */
data class UpdateHostSlackRequest(
    @field:Schema(defaultValue = "https://slack.dd.com", description = "슬랙 웹훅 URL")
    @field:NotBlank(message = "올바른 슬랙 URL 을 입력해주세요")
    @field:URL(message = "올바른 슬랙 URL 을 입력해주세요")
    val slackUrl: String,
)
