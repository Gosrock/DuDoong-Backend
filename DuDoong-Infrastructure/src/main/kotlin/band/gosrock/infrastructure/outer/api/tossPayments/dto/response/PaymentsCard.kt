package band.gosrock.infrastructure.outer.api.tossPayments.dto.response

class PaymentsCard {
    var amount: Long? = null
    var issuerCode: CardCode? = null
    var acquirerCode: CardCode? = null
    var number: String? = null
    var installmentPlanMonths: Long? = null
    var approveNo: String? = null
    var useCardPoint: Boolean? = null
    var cardType: String? = null
    var ownerType: String? = null
    var acquireStatus: CardAcquireStatus? = null
    var isInterestFree: Boolean? = null
    var interestPayer: String? = null
}
