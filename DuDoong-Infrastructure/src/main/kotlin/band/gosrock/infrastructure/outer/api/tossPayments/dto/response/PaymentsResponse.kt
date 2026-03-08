package band.gosrock.infrastructure.outer.api.tossPayments.dto.response

import java.time.ZonedDateTime

class PaymentsResponse {
    var version: String? = null
    var paymentKey: String? = null
    var type: String? = null
    var orderId: String? = null
    var orderName: String? = null
    var mId: String? = null
    var currency: String? = null
    var method: TossPaymentMethod? = null
    var totalAmount: Long? = null
    var balanceAmount: Long? = null
    var status: PaymentStatus? = null
    var requestedAt: ZonedDateTime? = null
    var approvedAt: ZonedDateTime? = null
    var useEscrow: Boolean? = null
    var lastTransactionKey: String? = null
    var suppliedAmount: Long? = null
    var vat: Long? = null
    var cultureExpense: Boolean? = null
    var taxFreeAmount: Long? = null
    var taxExemptionAmount: Long? = null
    var cancels: List<PaymentsCancels>? = null
    var isPartialCancelable: Boolean? = null
    var receipt: PaymentReceipt? = null
    var checkout: PaymentCheckout? = null
    var easyPay: PaymentEasyPay? = null
    var card: PaymentsCard? = null
    var country: String? = null
    var failure: PaymentsFailure? = null
    var cashReceipt: PaymentsCashReceipt? = null
    var discount: PaymentsCardPromotion? = null

    fun getProviderName(): String {
        return when (method) {
            TossPaymentMethod.CARD -> card?.issuerCode?.kr ?: ""
            TossPaymentMethod.EASYPAY -> easyPay?.provider?.kr ?: ""
            else -> ""
        }
    }
}
