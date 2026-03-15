package band.gosrock.domain.domains.coupon.exception

import band.gosrock.common.exception.DuDoongCodeException

class CouponCampaignNotFoundException private constructor() : DuDoongCodeException(CouponErrorCode.NOT_FOUND_COUPON_CAMPAIGN) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CouponCampaignNotFoundException()
    }
}
