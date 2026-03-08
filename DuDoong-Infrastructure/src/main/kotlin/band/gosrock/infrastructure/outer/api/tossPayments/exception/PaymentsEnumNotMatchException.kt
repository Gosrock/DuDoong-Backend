package band.gosrock.infrastructure.outer.api.tossPayments.exception

import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.common.exception.GlobalErrorCode

class PaymentsEnumNotMatchException private constructor() :
    DuDoongCodeException(GlobalErrorCode.TOSS_PAYMENTS_ENUM_NOT_MATCH) {
    companion object {
        @JvmField val EXCEPTION: DuDoongCodeException = PaymentsEnumNotMatchException()
    }
}
