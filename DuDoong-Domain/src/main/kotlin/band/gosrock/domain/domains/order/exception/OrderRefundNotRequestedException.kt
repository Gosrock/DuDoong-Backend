package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class OrderRefundNotRequestedException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_REFUND_NOT_REQUESTED) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = OrderRefundNotRequestedException()
    }
}
