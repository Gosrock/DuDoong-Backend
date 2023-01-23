package band.gosrock.domain.domains.coupon.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor;
import band.gosrock.domain.domains.coupon.domain.CouponCampaign;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class CreateIssuedCouponDomainService {

    private final IssuedCouponAdaptor issuedCouponAdaptor;

    @RedissonLock(LockName = "유저쿠폰발급", identifier = "id", paramClassType = CouponCampaign.class)
    public IssuedCoupon createIssuedCoupon(
            IssuedCoupon issuedCoupon, CouponCampaign couponCampaign) {
        // 이미 해당 유저가 쿠폰 발급했는지 검증
        issuedCouponAdaptor.exist(couponCampaign.getId(), issuedCoupon.getUserId());
        // 발급 가능 시간인지 검증
        couponCampaign.validateIssuePeriod();
        // 재고 감소 로직
        couponCampaign.decreaseCouponStock();
        // 쿠폰 발급
        return issuedCouponAdaptor.save(issuedCoupon);
    }
}
