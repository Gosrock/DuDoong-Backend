package band.gosrock.api.host.model.dto.request

import band.gosrock.infrastructure.config.slack.SlackMessageProvider
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/** 호스트 슬랙 알람 URL 수정 요청 DTO. Slack Incoming Webhook 주소(https://hooks.slack.com/services/...)만 받는다 (#764) */
data class UpdateHostSlackRequest(
    @field:Schema(defaultValue = "https://hooks.slack.com/services/T000/B000/XXXX", description = "슬랙 웹훅 URL")
    @field:NotBlank(message = "올바른 슬랙 URL 을 입력해주세요")
    @field:Pattern(regexp = SlackMessageProvider.SLACK_WEBHOOK_URL_PATTERN, message = "올바른 슬랙 URL 을 입력해주세요")
    val slackUrl: String,
)
