package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class CanNotCancelOrderException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_CANNOT_CANCEL) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CanNotCancelOrderException()
    }
}
