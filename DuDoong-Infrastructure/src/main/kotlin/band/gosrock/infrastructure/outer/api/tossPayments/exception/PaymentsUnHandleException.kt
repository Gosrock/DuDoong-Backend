package band.gosrock.infrastructure.outer.api.tossPayments.exception

import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.common.exception.GlobalErrorCode

class PaymentsUnHandleException private constructor() :
    DuDoongCodeException(GlobalErrorCode.TOSS_PAYMENTS_UNHANDLED) {
    companion object {
        @JvmField val EXCEPTION: DuDoongCodeException = PaymentsUnHandleException()
    }
}
