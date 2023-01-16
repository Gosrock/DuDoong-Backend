package band.gosrock.api.coupon.dto.response;


import band.gosrock.domain.domains.coupon.domain.CouponCampaign;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record CreateCouponCampaignResponse(
        @Schema(description = "쿠폰 캠페인 ID") Long couponCampaignId,
        @Schema(description = "쿠폰 코드") String couponCode,
        @Schema(description = "생성한 쿠폰 총 매수") Long issuedAmount,
        @Schema(description = "쿠폰 생성한 호스트 ID") Long hostId) {

    /*
    나중에 디자인 나오고 더 필요한 리스폰스 값 있으면 추가할 예정입니다.
    */

    public static CreateCouponCampaignResponse of(CouponCampaign couponCampaign, Long hostId) {
        return CreateCouponCampaignResponse.builder()
                .couponCampaignId(couponCampaign.getId())
                .couponCode(couponCampaign.getCouponCode())
                .issuedAmount(couponCampaign.getCouponStockInfo().getIssuedAmount())
                .hostId(hostId)
                .build();
    }
}
