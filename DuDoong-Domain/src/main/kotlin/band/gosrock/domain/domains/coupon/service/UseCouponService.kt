package band.gosrock.domain.domains.coupon.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor

@DomainService
class UseCouponService(
    private val issuedCouponAdaptor: IssuedCouponAdaptor,
) {
    /** 쿠폰 사용 (v1 주문 생성 BEFORE_COMMIT 핸들러). 락 키는 발급 쿠폰 id — identifier 가 실제 파라미터 이름과 달라 늘 BadLockIdentifier(AOP_500_1)였다 (#746) */
    @RedissonLock(LockName = "쿠폰", identifier = "issuedCouponId")
    fun execute(userId: Long, issuedCouponId: Long): Long {
        val coupon = issuedCouponAdaptor.query(issuedCouponId)
        coupon.validMine(userId)
        coupon.use()
        return issuedCouponId
    }
}
