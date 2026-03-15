package band.gosrock.api.coupon.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class CreateCouponCampaignResponse(
    @Schema(description = "쿠폰 캠페인 ID") val couponCampaignId: Long,
    @Schema(description = "쿠폰 코드") val couponCode: String,
    @Schema(description = "생성한 쿠폰 총 매수") val issuedAmount: Long,
    @Schema(description = "쿠폰 생성한 슈퍼 어드민 user ID") val userId: Long,
)
