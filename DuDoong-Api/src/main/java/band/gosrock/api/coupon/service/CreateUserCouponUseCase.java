package band.gosrock.api.coupon.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.coupon.dto.response.CreateUserCouponResponse;
import band.gosrock.api.coupon.mapper.IssuedCouponMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.domains.coupon.adaptor.CouponCampaignAdaptor;
import band.gosrock.domain.domains.coupon.domain.CouponCampaign;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import band.gosrock.domain.domains.coupon.service.CreateIssuedCouponDomainService;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateUserCouponUseCase {
    private final UserUtils userUtils;
    private final IssuedCouponMapper issuedCouponMapper;
    private final CouponCampaignAdaptor couponCampaignAdaptor;

    private final CreateIssuedCouponDomainService createIssuedCouponDomainService;

    @Transactional
    @RedissonLock(LockName = "유저쿠폰발급", identifier = "couponCode")
    public CreateUserCouponResponse execute(String couponCode) {
        // 존재하는 유저인지 검증
        User user = userUtils.getCurrentUser();
        // 쿠폰 코드 검증
        CouponCampaign couponCampaign = couponCampaignAdaptor.findByCouponCode(couponCode);
        // 재고 감소 및 쿠폰 발급
        IssuedCoupon issuedCoupon =
                createIssuedCouponDomainService.createIssuedCoupon(
                        issuedCouponMapper.toEntity(couponCampaign, user.getId()));
        return issuedCouponMapper.toCreateUserCouponResponse(issuedCoupon, couponCampaign);
    }
}
