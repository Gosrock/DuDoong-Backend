package band.gosrock.api.order.model.dto.request

import jakarta.validation.constraints.NotBlank

data class RefundRequest(
    @field:NotBlank
    val reason: String,
)
