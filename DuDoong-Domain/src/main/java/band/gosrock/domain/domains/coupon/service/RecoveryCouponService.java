package band.gosrock.domain.domains.coupon.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class RecoveryCouponService {
    private final IssuedCouponAdaptor issuedCouponAdaptor;

    @RedissonLock(LockName = "쿠폰", identifier = "couponId")
    public Long execute(Long userId, Long couponId) {
        IssuedCoupon coupon = issuedCouponAdaptor.query(couponId);
        coupon.validMine(userId);
        coupon.recovery();
        return couponId;
    }
}
