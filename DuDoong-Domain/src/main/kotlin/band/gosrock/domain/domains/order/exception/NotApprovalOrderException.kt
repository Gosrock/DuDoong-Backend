package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class NotApprovalOrderException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_NOT_APPROVAL) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = NotApprovalOrderException()
    }
}
