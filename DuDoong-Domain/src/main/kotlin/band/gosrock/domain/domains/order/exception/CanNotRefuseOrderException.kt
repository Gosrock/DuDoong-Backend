package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class CanNotRefuseOrderException private constructor() : DuDoongCodeException(OrderErrorCode.ORDER_CANNOT_REFUSE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CanNotRefuseOrderException()
    }
}
