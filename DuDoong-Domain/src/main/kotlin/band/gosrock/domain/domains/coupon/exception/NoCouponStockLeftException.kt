package band.gosrock.domain.domains.coupon.exception

import band.gosrock.common.exception.DuDoongCodeException

class NoCouponStockLeftException private constructor() : DuDoongCodeException(CouponErrorCode.NO_COUPON_STOCK_LEFT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = NoCouponStockLeftException()
    }
}
