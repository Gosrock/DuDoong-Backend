package band.gosrock.domain.domains.coupon.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.coupon.adaptor.CouponCampaignAdaptor
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon

@DomainService
class CreateIssuedCouponDomainService(
    private val issuedCouponAdaptor: IssuedCouponAdaptor,
    private val couponCampaignAdaptor: CouponCampaignAdaptor,
) {
    /**
     * 쿠폰 발급. `유저쿠폰발급:{캠페인 id}` 락의 새 트랜잭션 안에서 캠페인을 **다시 읽어** 재고를 줄이고 발급 쿠폰과 함께 커밋한다 (#746).
     * 예전에는 호출 측(트랜잭션)이 락 밖에서 읽은 캠페인 엔티티의 재고를 줄여, 락이 풀린 뒤 호출 측 커밋 때 옛 값 기준으로 덮어썼다 — 동시 발급 시 재고 감소 유실
     */
    @RedissonLock(LockName = "유저쿠폰발급", identifier = "couponCampaignId")
    fun createIssuedCoupon(userId: Long, couponCampaignId: Long): IssuedCoupon {
        val couponCampaign = couponCampaignAdaptor.queryCouponCampaign(couponCampaignId)
        issuedCouponAdaptor.exist(couponCampaignId, userId)
        couponCampaign.validateIssuePeriod()
        couponCampaign.decreaseCouponStock()
        return issuedCouponAdaptor.save(IssuedCoupon(couponCampaign = couponCampaign, userId = userId))
    }
}
