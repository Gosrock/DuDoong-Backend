package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class OrderNotFoundException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_NOT_FOUND) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = OrderNotFoundException()
    }
}
