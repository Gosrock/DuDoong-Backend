package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class NotRefundAvailableDateOrderException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_NOT_REFUND_DATE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = NotRefundAvailableDateOrderException()
    }
}
