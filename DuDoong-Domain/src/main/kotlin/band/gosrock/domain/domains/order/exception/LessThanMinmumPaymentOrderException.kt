package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class LessThanMinmumPaymentOrderException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_LESS_THAN_MINIMUM) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = LessThanMinmumPaymentOrderException()
    }
}
