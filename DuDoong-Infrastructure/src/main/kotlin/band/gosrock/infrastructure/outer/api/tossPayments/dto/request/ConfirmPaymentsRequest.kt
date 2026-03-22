package band.gosrock.infrastructure.outer.api.tossPayments.dto.request

data class ConfirmPaymentsRequest(
    val paymentKey: String? = null,
    val orderId: String? = null,
    val amount: Long? = null,
)
