package band.gosrock.domain.domains.coupon.exception

import band.gosrock.common.exception.DuDoongCodeException

class AlreadyExistCouponCampaignException private constructor() : DuDoongCodeException(CouponErrorCode.DUPLICATE_COUPON_CODE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = AlreadyExistCouponCampaignException()
    }
}
