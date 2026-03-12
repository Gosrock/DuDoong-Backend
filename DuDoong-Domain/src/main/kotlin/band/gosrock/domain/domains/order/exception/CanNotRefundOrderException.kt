package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class CanNotRefundOrderException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_CANNOT_REFUND) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CanNotRefundOrderException()
    }
}
