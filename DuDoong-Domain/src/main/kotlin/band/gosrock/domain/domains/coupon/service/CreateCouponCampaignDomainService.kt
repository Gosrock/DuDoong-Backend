package band.gosrock.domain.domains.coupon.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.domains.coupon.adaptor.CouponCampaignAdaptor
import band.gosrock.domain.domains.coupon.domain.CouponCampaign
import org.springframework.transaction.annotation.Transactional

@DomainService
@Transactional(readOnly = true)
class CreateCouponCampaignDomainService(
    private val couponCampaignAdaptor: CouponCampaignAdaptor,
) {
    fun checkCouponCodeExists(couponCode: String) {
        couponCampaignAdaptor.existsByCouponCode(couponCode)
    }

    @Transactional
    fun createCouponCampaign(couponCampaign: CouponCampaign): CouponCampaign {
        couponCampaign.validatePercentageAmount(couponCampaign.discountType!!, couponCampaign.discountAmount!!)
        return couponCampaignAdaptor.save(couponCampaign)
    }
}
