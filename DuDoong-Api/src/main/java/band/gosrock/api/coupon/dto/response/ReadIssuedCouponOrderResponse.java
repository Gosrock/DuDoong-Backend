package band.gosrock.api.coupon.dto.response;


import band.gosrock.domain.common.vo.IssuedCouponInfoVo;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReadIssuedCouponOrderResponse {

    @Schema(description = "총 개수")
    private final Long totalNum;

    @Schema(description = "쿠폰 정보 리스트")
    private final List<IssuedCouponInfoVo> issuedCouponInfoList;

    public static ReadIssuedCouponOrderResponse of(List<IssuedCoupon> issuedCoupons) {
        if (issuedCoupons.isEmpty()) {
            return ReadIssuedCouponOrderResponse.builder()
                    .totalNum(0L)
                    .issuedCouponInfoList(new ArrayList<>())
                    .build();
        }
        return ReadIssuedCouponOrderResponse.builder()
                .totalNum((long) issuedCoupons.size())
                .issuedCouponInfoList(issuedCoupons.stream().map(IssuedCouponInfoVo::of).toList())
                .build();
    }
}
