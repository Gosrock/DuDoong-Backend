package band.gosrock.domain.domains.coupon.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor
import band.gosrock.domain.domains.coupon.domain.CouponCampaign
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon

@DomainService
class CreateIssuedCouponDomainService(
    private val issuedCouponAdaptor: IssuedCouponAdaptor,
) {
    @RedissonLock(LockName = "유저쿠폰발급", identifier = "id", paramClassType = CouponCampaign::class)
    fun createIssuedCoupon(issuedCoupon: IssuedCoupon, couponCampaign: CouponCampaign): IssuedCoupon {
        issuedCouponAdaptor.exist(couponCampaign.id!!, issuedCoupon.userId!!)
        couponCampaign.validateIssuePeriod()
        couponCampaign.decreaseCouponStock()
        return issuedCouponAdaptor.save(issuedCoupon)
    }
}
