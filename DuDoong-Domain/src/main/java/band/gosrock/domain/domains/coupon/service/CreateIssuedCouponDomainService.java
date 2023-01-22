package band.gosrock.domain.domains.coupon.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class CreateIssuedCouponDomainService {

    private final IssuedCouponAdaptor issuedCouponAdaptor;

    public IssuedCoupon createIssuedCoupon(IssuedCoupon issuedCoupon) {
        // 이미 해당 유저가 쿠폰 발급했는지 검증
        issuedCouponAdaptor.checkIssuedCouponExists(
                issuedCoupon.getCouponCampaign(), issuedCoupon.getUserId());
        // 재고 감소 로직
        issuedCoupon.getCouponCampaign().decreaseCouponStock();
        // 쿠폰 발급
        return issuedCouponAdaptor.save(issuedCoupon);
    }
}
