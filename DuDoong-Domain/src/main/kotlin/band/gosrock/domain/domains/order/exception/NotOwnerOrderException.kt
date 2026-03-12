package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class NotOwnerOrderException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_NOT_MINE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = NotOwnerOrderException()
    }
}
