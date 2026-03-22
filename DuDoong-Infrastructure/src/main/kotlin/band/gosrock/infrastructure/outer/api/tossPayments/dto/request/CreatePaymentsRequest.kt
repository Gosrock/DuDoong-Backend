package band.gosrock.infrastructure.outer.api.tossPayments.dto.request

data class CreatePaymentsRequest(
    val method: String? = null,
    val amount: Long? = null,
    val orderId: String? = null,
    val orderName: String? = null,
    val successUrl: String? = null,
    val failUrl: String? = null,
)
