package band.gosrock.admin.model.dto.request

import jakarta.validation.constraints.NotNull

data class AdminRefundStatusRequest(
    @field:NotNull
    val refundStatus: String,
    val reason: String? = null,
)
