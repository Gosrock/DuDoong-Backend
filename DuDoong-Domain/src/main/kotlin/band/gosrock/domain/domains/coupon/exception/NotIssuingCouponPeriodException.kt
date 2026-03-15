package band.gosrock.domain.domains.coupon.exception

import band.gosrock.common.exception.DuDoongCodeException

class NotIssuingCouponPeriodException private constructor() : DuDoongCodeException(CouponErrorCode.NOT_COUPON_ISSUING_PERIOD) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = NotIssuingCouponPeriodException()
    }
}
