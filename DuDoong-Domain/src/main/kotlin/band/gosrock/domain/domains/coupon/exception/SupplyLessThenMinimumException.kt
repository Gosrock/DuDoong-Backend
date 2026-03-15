package band.gosrock.domain.domains.coupon.exception

import band.gosrock.common.exception.DuDoongCodeException

class SupplyLessThenMinimumException private constructor() : DuDoongCodeException(CouponErrorCode.SUPPLY_LESS_THEN_MINIMUM) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = SupplyLessThenMinimumException()
    }
}
