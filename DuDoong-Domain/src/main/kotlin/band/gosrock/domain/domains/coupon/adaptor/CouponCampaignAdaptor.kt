package band.gosrock.domain.domains.coupon.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.coupon.domain.CouponCampaign
import band.gosrock.domain.domains.coupon.exception.AlreadyExistCouponCampaignException
import band.gosrock.domain.domains.coupon.exception.CouponCampaignNotFoundException
import band.gosrock.domain.domains.coupon.repository.CouponCampaignRepository

@Adaptor
class CouponCampaignAdaptor(
    private val couponCampaignRepository: CouponCampaignRepository,
) {
    fun save(couponCampaign: CouponCampaign): CouponCampaign =
        couponCampaignRepository.save(couponCampaign)

    fun existsByCouponCode(couponCode: String) {
        if (couponCampaignRepository.existsByCouponCode(couponCode)) {
            throw AlreadyExistCouponCampaignException.EXCEPTION
        }
    }

    fun findByCouponCode(couponCode: String): CouponCampaign =
        couponCampaignRepository.findByCouponCode(couponCode)
            .orElseThrow { CouponCampaignNotFoundException.EXCEPTION }
}
