package band.gosrock.domain.domains.coupon.exception

import band.gosrock.common.exception.DuDoongCodeException

class SupplyLessThenDiscountException private constructor() : DuDoongCodeException(CouponErrorCode.SUPPLY_LESS_THEN_DISCOUNT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = SupplyLessThenDiscountException()
    }
}
