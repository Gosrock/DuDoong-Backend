package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class OrderLineNotFountException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_LINE_NOT_FOUND) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = OrderLineNotFountException()
    }
}
