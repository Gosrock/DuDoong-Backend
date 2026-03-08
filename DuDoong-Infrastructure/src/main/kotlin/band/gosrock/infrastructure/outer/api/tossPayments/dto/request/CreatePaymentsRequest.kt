package band.gosrock.infrastructure.outer.api.tossPayments.dto.request

data class CreatePaymentsRequest(
    val method: String? = null,
    val amount: Long? = null,
    val orderId: String? = null,
    val orderName: String? = null,
    val successUrl: String? = null,
    val failUrl: String? = null,
) {
    class Builder {
        private var method: String? = null
        private var amount: Long? = null
        private var orderId: String? = null
        private var orderName: String? = null
        private var successUrl: String? = null
        private var failUrl: String? = null
        fun method(v: String?) = apply { method = v }
        fun amount(v: Long?) = apply { amount = v }
        fun orderId(v: String?) = apply { orderId = v }
        fun orderName(v: String?) = apply { orderName = v }
        fun successUrl(v: String?) = apply { successUrl = v }
        fun failUrl(v: String?) = apply { failUrl = v }
        fun build() = CreatePaymentsRequest(method, amount, orderId, orderName, successUrl, failUrl)
    }

    companion object {
        @JvmStatic fun builder() = Builder()
    }
}
