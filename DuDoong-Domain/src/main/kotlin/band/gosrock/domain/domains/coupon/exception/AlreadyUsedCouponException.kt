package band.gosrock.domain.domains.coupon.exception

import band.gosrock.common.exception.DuDoongCodeException

class AlreadyUsedCouponException private constructor() : DuDoongCodeException(CouponErrorCode.ALREADY_USED_COUPON) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = AlreadyUsedCouponException()
    }
}
