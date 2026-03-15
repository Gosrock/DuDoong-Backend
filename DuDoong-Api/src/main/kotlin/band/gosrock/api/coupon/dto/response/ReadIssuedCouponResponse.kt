package band.gosrock.api.coupon.dto.response

import band.gosrock.domain.common.vo.IssuedCouponInfoVo
import io.swagger.v3.oas.annotations.media.Schema

data class ReadIssuedCouponResponse(
    @Schema(description = "사용 가능 쿠폰 총 개수") val availableCouponNum: Long,
    @Schema(description = "사용 가능 쿠폰 정보 리스트") val availableCouponInfoList: List<IssuedCouponInfoVo>,
    @Schema(description = "사용됨/기한만료 쿠폰 총 개수") val expiredCouponNum: Long,
    @Schema(description = "사용됨/기한만료 쿠폰 정보 리스트") val expiredCouponInfoList: List<IssuedCouponInfoVo>,
)
