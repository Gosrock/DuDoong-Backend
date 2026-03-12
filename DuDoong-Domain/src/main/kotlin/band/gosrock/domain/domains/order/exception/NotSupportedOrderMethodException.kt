package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class NotSupportedOrderMethodException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_NOT_SUPPORTED_METHOD) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = NotSupportedOrderMethodException()
    }
}
