package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

/** v2 사용자 주문(#718) 예외 */
class V2UnsupportedOrderTicketException private constructor() : DuDoongCodeException(OrderErrorCode.V2_UNSUPPORTED_ORDER_TICKET) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = V2UnsupportedOrderTicketException()
    }
}

class V2InvalidPaymentMethodException private constructor() : DuDoongCodeException(OrderErrorCode.V2_INVALID_PAYMENT_METHOD) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = V2InvalidPaymentMethodException()
    }
}

class V2InvalidDepositorNameException private constructor() : DuDoongCodeException(OrderErrorCode.V2_INVALID_DEPOSITOR_NAME) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = V2InvalidDepositorNameException()
    }
}

class V2InvalidOptionAnswersException private constructor() : DuDoongCodeException(OrderErrorCode.V2_INVALID_OPTION_ANSWERS) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = V2InvalidOptionAnswersException()
    }
}

class V2OrderCannotCancelByUserException private constructor() : DuDoongCodeException(OrderErrorCode.V2_ORDER_CANNOT_CANCEL_BY_USER) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = V2OrderCannotCancelByUserException()
    }
}

class V2RefundAccountRequiredException private constructor() : DuDoongCodeException(OrderErrorCode.V2_REFUND_ACCOUNT_REQUIRED) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = V2RefundAccountRequiredException()
    }
}

class V2DuplicateOrderInProgressException private constructor() : DuDoongCodeException(OrderErrorCode.V2_DUPLICATE_ORDER_IN_PROGRESS) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = V2DuplicateOrderInProgressException()
    }
}
