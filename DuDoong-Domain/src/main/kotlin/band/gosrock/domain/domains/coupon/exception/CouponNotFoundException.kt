package band.gosrock.domain.domains.coupon.exception

import band.gosrock.common.exception.DuDoongCodeException

class CouponNotFoundException private constructor() : DuDoongCodeException(CouponErrorCode.NOT_FOUND_COUPON) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CouponNotFoundException()
    }
}
