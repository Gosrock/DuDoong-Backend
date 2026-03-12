package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class ApproveWaitingOrderPurchaseLimitException private constructor() : DuDoongCodeException(OrderErrorCode.APPROVE_WAITING_PURCHASE_LIMIT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = ApproveWaitingOrderPurchaseLimitException()
    }
}
