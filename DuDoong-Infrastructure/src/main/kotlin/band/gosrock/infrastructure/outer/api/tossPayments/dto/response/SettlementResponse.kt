package band.gosrock.infrastructure.outer.api.tossPayments.dto.response

import java.time.LocalDate
import java.time.ZonedDateTime

class SettlementResponse {
    var paymentKey: String? = null
    var transactionKey: String? = null
    var orderId: String? = null
    var orderName: String? = null
    var mId: String? = null
    var currency: String? = null
    var method: TossPaymentMethod? = null
    var amount: Long? = null
    var supplyAmount: Long? = null
    var vat: Long? = null
    var payOutAmount: Long? = null
    var interestFee: Long? = null
    var approvedAt: ZonedDateTime? = null
    var soldDate: LocalDate? = null
    var paidOutDate: LocalDate? = null
    var fees: List<SettlementFeeDto>? = null
}
