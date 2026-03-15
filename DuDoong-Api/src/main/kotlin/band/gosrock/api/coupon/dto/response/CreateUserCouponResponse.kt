package band.gosrock.api.coupon.dto.response

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.coupon.domain.DiscountType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class CreateUserCouponResponse(
    @Schema(description = "발급한 쿠폰 id") val issuedCouponId: Long,
    @Schema(description = "쿠폰 캠페인 id") val couponCampaignId: Long,
    @Schema(description = "쿠폰 코드") val couponCode: String,
    @Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "쿠폰 유효 기간")
    @DateFormat
    val validTerm: LocalDateTime,
    @Schema(description = "할인타입(정액,정률)") val discountType: DiscountType,
    @Schema(description = "할인량") val discountAmount: Long,
)
