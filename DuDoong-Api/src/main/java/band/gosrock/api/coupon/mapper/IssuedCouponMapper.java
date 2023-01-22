package band.gosrock.api.coupon.mapper;


import band.gosrock.api.coupon.dto.response.CreateUserCouponResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor;
import band.gosrock.domain.domains.coupon.domain.CouponCampaign;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
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
}
