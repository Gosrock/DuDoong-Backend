package band.gosrock.api.coupon.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record CreateCouponCampaignResponse(
        @Schema(description = "쿠폰 캠페인 ID") Long couponCampaignId,
        @Schema(description = "쿠폰 코드") String couponCode,
        @Schema(description = "생성한 쿠폰 총 매수") Long issuedAmount,
        @Schema(description = "쿠폰 생성한 슈퍼 어드민 user ID") Long userId) {}
