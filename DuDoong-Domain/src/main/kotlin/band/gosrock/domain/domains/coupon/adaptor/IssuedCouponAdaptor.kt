package band.gosrock.domain.domains.coupon.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon
import band.gosrock.domain.domains.coupon.exception.AlreadyIssuedCouponException
import band.gosrock.domain.domains.coupon.exception.CouponNotFoundException
import band.gosrock.domain.domains.coupon.repository.IssuedCouponRepository

@Adaptor
class IssuedCouponAdaptor(
    private val issuedCouponRepository: IssuedCouponRepository,
) {
    fun findAllByUserId(userId: Long): List<IssuedCoupon> =
        issuedCouponRepository.findAllByUserId(userId)

    fun save(issuedCoupon: IssuedCoupon): IssuedCoupon =
        issuedCouponRepository.save(issuedCoupon)

    fun exist(couponCampaignId: Long, userId: Long) {
        issuedCouponRepository.findByCouponCampaignIdAndUserId(couponCampaignId, userId)
            .ifPresent { throw AlreadyIssuedCouponException.EXCEPTION }
    }

    fun query(issuedCouponId: Long): IssuedCoupon =
        issuedCouponRepository.findById(issuedCouponId)
            .orElseThrow { CouponNotFoundException.EXCEPTION }
}
