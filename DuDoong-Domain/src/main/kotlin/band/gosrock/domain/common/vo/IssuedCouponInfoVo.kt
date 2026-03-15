package band.gosrock.domain.common.vo

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.coupon.domain.ApplyTarget
import band.gosrock.domain.domains.coupon.domain.DiscountType
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon
import java.time.LocalDateTime

data class IssuedCouponInfoVo(
    val issuedCouponId: Long? = null,
    // 사용여부
    val usageStatus: Boolean? = null,
    val applyTarget: ApplyTarget? = null,
    val couponCode: String? = null,
    // 정률할인, 정액할인
    val discountType: DiscountType? = null,
    val discountAmount: Long? = null,
    // 쿠폰 사용 가능 마감 시각
    @DateFormat val validDateTime: LocalDateTime? = null,
    val minimumCost: Long? = null,
) {
    companion object {
        @JvmStatic
        fun of(issuedCoupon: IssuedCoupon): IssuedCouponInfoVo =
            IssuedCouponInfoVo(
                issuedCouponId = issuedCoupon.id,
                usageStatus = issuedCoupon.usageStatus,
                applyTarget = issuedCoupon.couponCampaign?.applyTarget,
                couponCode = issuedCoupon.couponCampaign?.couponCode,
                discountType = issuedCoupon.couponCampaign?.discountType,
                discountAmount = issuedCoupon.couponCampaign?.discountAmount,
                validDateTime = issuedCoupon.calculateValidTerm(),
                minimumCost = issuedCoupon.couponCampaign?.minimumCost,
            )
    }
}
