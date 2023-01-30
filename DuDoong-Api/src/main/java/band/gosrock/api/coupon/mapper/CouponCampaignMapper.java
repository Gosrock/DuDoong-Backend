package band.gosrock.api.coupon.mapper;


import band.gosrock.api.coupon.dto.reqeust.CreateCouponCampaignRequest;
import band.gosrock.api.coupon.dto.response.CreateCouponCampaignResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.common.vo.DateTimePeriod;
import band.gosrock.domain.domains.coupon.domain.CouponCampaign;
import band.gosrock.domain.domains.coupon.domain.CouponStockInfo;
import java.time.LocalDateTime;
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

    public static CouponStockInfo toCouponStockInfo(Long IssuedAmount) {
        return CouponStockInfo.builder()
                .issuedAmount(IssuedAmount)
                .remainingAmount(IssuedAmount)
                .build();
    }

    public static DateTimePeriod toDateTimePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        return DateTimePeriod.builder().startAt(startAt).endAt(endAt).build();
    }

    public CouponCampaign toEntity(CreateCouponCampaignRequest createCouponCampaignRequest) {

        CouponStockInfo couponStockInfo =
                toCouponStockInfo(createCouponCampaignRequest.getIssuedAmount());
        DateTimePeriod dateTimePeriod =
                toDateTimePeriod(
                        createCouponCampaignRequest.getStartAt(),
                        createCouponCampaignRequest.getEndAt());

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
