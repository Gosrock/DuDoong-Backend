package band.gosrock.domain.domains.coupon.exception

import band.gosrock.common.exception.DuDoongCodeException

class AlreadyRecoveredCouponException private constructor() : DuDoongCodeException(CouponErrorCode.ALREADY_RECOVERED_COUPON) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = AlreadyRecoveredCouponException()
    }
}
