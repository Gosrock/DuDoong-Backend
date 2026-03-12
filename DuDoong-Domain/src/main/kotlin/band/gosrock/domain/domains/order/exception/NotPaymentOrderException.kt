package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class NotPaymentOrderException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_NOT_PAYMENT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = NotPaymentOrderException()
    }
}
