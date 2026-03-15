package band.gosrock.api.event.model.dto.request

import band.gosrock.common.annotation.Enum
import band.gosrock.domain.domains.event.domain.EventStatus
import io.swagger.v3.oas.annotations.media.Schema

data class UpdateEventStatusRequest(
    @field:Schema(defaultValue = "OPEN", description = "오픈 상태")
    @field:Enum(message = "올바른 값을 입력해주세요.")
    val status: EventStatus? = null
)
