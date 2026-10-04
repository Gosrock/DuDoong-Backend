package band.gosrock.domain.domains.order.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidRefuseReasonException private constructor() : DuDoongCodeException(OrderErrorCode.INVALID_REFUSE_REASON) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidRefuseReasonException()
    }
}
