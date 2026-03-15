package band.gosrock.domain.domains.coupon.exception

import band.gosrock.common.exception.DuDoongCodeException

class AlreadyIssuedCouponException private constructor() : DuDoongCodeException(CouponErrorCode.ALREADY_ISSUED_COUPON) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = AlreadyIssuedCouponException()
    }
}
