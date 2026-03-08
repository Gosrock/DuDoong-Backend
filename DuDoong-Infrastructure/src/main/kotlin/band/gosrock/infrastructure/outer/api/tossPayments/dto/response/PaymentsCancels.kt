package band.gosrock.infrastructure.outer.api.tossPayments.dto.response

import java.time.ZonedDateTime

class PaymentsCancels {
    var cancelAmount: Long? = null
    var cancelReason: String? = null
    var taxFreeAmount: Long? = null
    var taxExceptionAmount: Long? = null
    var refundableAmount: Long? = null
    var easyPayDiscountAmount: Long? = null
    var canceledAt: ZonedDateTime? = null
    var transactionKey: String? = null
}
