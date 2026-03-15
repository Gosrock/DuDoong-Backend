package band.gosrock.api.coupon.dto.reqeust

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.coupon.domain.ApplyTarget
import band.gosrock.domain.domains.coupon.domain.DiscountType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import javax.validation.constraints.Future
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive

data class CreateCouponCampaignRequest(
    @field:NotNull(message = "discountType을 입력해주세요.")
    val discountType: DiscountType,

    @Schema(nullable = true, defaultValue = "ALL")
    val applyTarget: ApplyTarget? = null,

    @field:NotNull(message = "validTerm을 입력해주세요.")
    @field:Positive(message = "validTerm은 양수여야합니다.(유효기간)")
    val validTerm: Long,

    @Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "쿠폰 발행 시작 시간")
    @field:NotNull(message = "startAt을 입력해주세요.")
    @field:DateFormat
    val startAt: LocalDateTime,

    @Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "쿠폰 발행 마감 시간")
    @field:NotNull(message = "endAt을 입력해주세요.")
    @field:Future(message = "endAt은 값이 미래여야합니다.")
    @field:DateFormat
    val endAt: LocalDateTime,

    @field:NotNull(message = "issuedAmount을 입력해주세요.")
    @field:Positive(message = "issuedAmount는 양수여야합니다.")
    val issuedAmount: Long,

    @field:NotNull(message = "discountAmount을 입력해주세요.")
    @field:Positive(message = "discountAmount은 양수여야합니다.")
    val discountAmount: Long,

    @field:NotBlank(message = "couponCode를 입력해주세요.")
    val couponCode: String,

    @Schema(description = "쿠폰 사용 가능한 최소 결제 금액(원단위, 10000원 이상부터 입력 가능)")
    @field:NotNull(message = "minimumCost(원 단위)를 입력해주세요.")
    @field:Min(value = 10000, message = "10000원 이상부터 입력 가능합니다.")
    val minimumCost: Long,
)
