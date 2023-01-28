package band.gosrock.api.coupon.mapper;


import band.gosrock.api.coupon.dto.response.CreateUserCouponResponse;
import band.gosrock.api.coupon.dto.response.ReadIssuedCouponResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.common.vo.IssuedCouponInfoVo;
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor;
import band.gosrock.domain.domains.coupon.domain.CouponCampaign;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Mapper
@RequiredArgsConstructor
public class IssuedCouponMapper {

    private final IssuedCouponAdaptor issuedCouponAdaptor;

    public IssuedCoupon toEntity(CouponCampaign couponCampaign, Long userId) {
        return IssuedCoupon.builder().couponCampaign(couponCampaign).userId(userId).build();
    }

    public CreateUserCouponResponse toCreateUserCouponResponse(
            IssuedCoupon issuedCoupon, CouponCampaign couponCampaign) {
        return CreateUserCouponResponse.builder()
                .issuedCouponId(issuedCoupon.getId())
                .couponCampaignId(couponCampaign.getId())
                .couponCode(couponCampaign.getCouponCode())
                .validTerm(issuedCoupon.getCreatedAt().plusDays(couponCampaign.getValidTerm()))
                .discountType(couponCampaign.getDiscountType())
                .discountAmount(couponCampaign.getDiscountAmount())
                .build();
    }

    public ReadIssuedCouponResponse toReadIssuedCouponMyPageResponse(
            List<IssuedCoupon> availableIssuedCoupons, List<IssuedCoupon> expiredIssuedCoupons) {
        if (availableIssuedCoupons.isEmpty()) {
            availableIssuedCoupons = new ArrayList<>();
        }

        return ReadIssuedCouponResponse.builder()
                .availableCouponNum((long) availableIssuedCoupons.size())
                .availableCouponInfoList(
                        availableIssuedCoupons.stream().map(IssuedCouponInfoVo::of).toList())
                .expiredCouponNum((long) expiredIssuedCoupons.size())
                .expiredCouponInfoList(
                        expiredIssuedCoupons.stream().map(IssuedCouponInfoVo::of).toList())
                .build();
    }
}
