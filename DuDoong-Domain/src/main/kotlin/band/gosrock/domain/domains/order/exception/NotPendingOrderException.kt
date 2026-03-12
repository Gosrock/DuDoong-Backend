package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class NotPendingOrderException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_NOT_PENDING) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = NotPendingOrderException()
    }
}
