package band.gosrock.infrastructure.outer.api.tossPayments.dto.request

data class ConfirmPaymentsRequest(
    val paymentKey: String? = null,
    val orderId: String? = null,
    val amount: Long? = null,
) {
    class Builder {
        private var paymentKey: String? = null
        private var orderId: String? = null
        private var amount: Long? = null
        fun paymentKey(v: String?) = apply { paymentKey = v }
        fun orderId(v: String?) = apply { orderId = v }
        fun amount(v: Long?) = apply { amount = v }
        fun build() = ConfirmPaymentsRequest(paymentKey, orderId, amount)
    }

    companion object {
        @JvmStatic fun builder() = Builder()
    }
}
