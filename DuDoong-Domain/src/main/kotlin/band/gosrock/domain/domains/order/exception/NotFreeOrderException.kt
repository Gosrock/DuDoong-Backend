package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class NotFreeOrderException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_NOT_FREE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = NotFreeOrderException()
    }
}
