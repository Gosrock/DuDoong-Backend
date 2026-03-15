package band.gosrock.domain.domains.coupon.exception

import band.gosrock.common.exception.DuDoongCodeException

class NotMyCouponException private constructor() : DuDoongCodeException(CouponErrorCode.NOT_MY_COUPON) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = NotMyCouponException()
    }
}
