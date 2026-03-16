package band.gosrock.api.order.model.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class CreateOrderRequest(
    @Schema(nullable = true, defaultValue = "null")
    val couponId: Long?,

    @field:NotNull
    val cartId: Long?,
)
