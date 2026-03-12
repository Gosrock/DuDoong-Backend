package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidOrderException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_NOT_VALID) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidOrderException()
    }
}
