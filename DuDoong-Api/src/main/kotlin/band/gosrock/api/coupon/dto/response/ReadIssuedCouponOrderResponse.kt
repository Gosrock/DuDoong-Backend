package band.gosrock.api.coupon.dto.response

import band.gosrock.domain.common.vo.IssuedCouponInfoVo
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon
import io.swagger.v3.oas.annotations.media.Schema

data class ReadIssuedCouponOrderResponse(
    @Schema(description = "총 개수") val totalNum: Long,
    @Schema(description = "쿠폰 정보 리스트") val issuedCouponInfoList: List<IssuedCouponInfoVo>,
) {
    companion object {
        fun of(issuedCoupons: List<IssuedCoupon>): ReadIssuedCouponOrderResponse {
            if (issuedCoupons.isEmpty()) {
                return ReadIssuedCouponOrderResponse(totalNum = 0L, issuedCouponInfoList = emptyList())
            }
            return ReadIssuedCouponOrderResponse(
                totalNum = issuedCoupons.size.toLong(),
                issuedCouponInfoList = issuedCoupons.map { IssuedCouponInfoVo.of(it) },
            )
        }
    }
}
