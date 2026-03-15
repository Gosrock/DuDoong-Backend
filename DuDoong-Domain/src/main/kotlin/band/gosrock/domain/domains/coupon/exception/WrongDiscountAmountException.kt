package band.gosrock.domain.domains.coupon.exception

import band.gosrock.common.exception.DuDoongCodeException

class WrongDiscountAmountException private constructor() : DuDoongCodeException(CouponErrorCode.WRONG_DISCOUNT_AMOUNT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = WrongDiscountAmountException()
    }
}
