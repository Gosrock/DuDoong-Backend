package band.gosrock.domain.domains.coupon.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor

@DomainService
class UseCouponService(
    private val issuedCouponAdaptor: IssuedCouponAdaptor,
) {
    @RedissonLock(LockName = "쿠폰", identifier = "couponId")
    fun execute(userId: Long, issuedCouponId: Long): Long {
        val coupon = issuedCouponAdaptor.query(issuedCouponId)
        coupon.validMine(userId)
        coupon.use()
        return issuedCouponId
    }
}
