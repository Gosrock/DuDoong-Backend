package band.gosrock.api.coupon.mapper;


import band.gosrock.api.coupon.dto.reqeust.CreateCouponCampaignRequest;
import band.gosrock.api.coupon.dto.response.CreateCouponCampaignResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.common.vo.DateTimePeriod;
import band.gosrock.domain.domains.coupon.domain.CouponCampaign;
import band.gosrock.domain.domains.coupon.domain.CouponStockInfo;
import lombok.RequiredArgsConstructor;

@Mapper
@RequiredArgsConstructor
public class CouponCampaignMapper {

    public static CreateCouponCampaignResponse toCreateCouponCampaignResponse(
            CouponCampaign couponCampaign, Long hostId) {
        return CreateCouponCampaignResponse.builder()
                .couponCampaignId(couponCampaign.getId())
                .couponCode(couponCampaign.getCouponCode())
                .issuedAmount(couponCampaign.getCouponStockInfo().getIssuedAmount())
                .hostId(hostId)
                .build();
    }

    public CouponCampaign toEntity(CreateCouponCampaignRequest createCouponCampaignRequest) {

        CouponStockInfo couponStockInfo =
                CouponStockInfo.builder()
                        .issuedAmount(createCouponCampaignRequest.getIssuedAmount())
                        .remainingAmount(createCouponCampaignRequest.getIssuedAmount())
                        .build();
        DateTimePeriod dateTimePeriod =
                DateTimePeriod.builder()
                        .startAt(createCouponCampaignRequest.getStartAt())
                        .endAt(createCouponCampaignRequest.getEndAt())
                        .build();

        return CouponCampaign.builder()
                .hostId(createCouponCampaignRequest.getHostId())
                .discountType(createCouponCampaignRequest.getDiscountType())
                .applyTarget(createCouponCampaignRequest.getApplyTarget())
                .validTerm(createCouponCampaignRequest.getValidTerm())
                .dateTimePeriod(dateTimePeriod)
                .couponStockInfo(couponStockInfo)
                .discountAmount(createCouponCampaignRequest.getDiscountAmount())
                .couponCode(createCouponCampaignRequest.getCouponCode())
                .build();
    }
}
