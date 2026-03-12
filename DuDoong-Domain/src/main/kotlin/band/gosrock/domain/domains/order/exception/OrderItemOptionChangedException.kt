package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class OrderItemOptionChangedException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_OPTION_CHANGED) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = OrderItemOptionChangedException()
    }
}
