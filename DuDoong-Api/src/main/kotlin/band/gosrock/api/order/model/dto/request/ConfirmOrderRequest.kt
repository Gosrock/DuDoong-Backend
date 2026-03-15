package band.gosrock.api.order.model.dto.request

data class ConfirmOrderRequest(
    val paymentKey: String?,
    val amount: Long?,
)
