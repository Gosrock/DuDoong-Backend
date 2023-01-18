package band.gosrock.api.coupon.dto.response;


import band.gosrock.domain.common.vo.IssuedCouponInfoVo;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReadIssuedCouponOrderResponse {

    @Schema(description = "총 개수")
    private final Long totalNum;

    @Schema(description = "쿠폰 정보 리스트")
    private final List<IssuedCouponInfoVo> issuedCouponInfoList;

    public static ReadIssuedCouponOrderResponse of(Optional<List<IssuedCoupon>> issuedCoupons) {
        if (issuedCoupons.isPresent()) {
            return ReadIssuedCouponOrderResponse.builder()
                    .totalNum((long) issuedCoupons.get().size())
                    .issuedCouponInfoList(
                            issuedCoupons.get().stream().map(IssuedCouponInfoVo::of).toList())
                    .build();
        }
        return ReadIssuedCouponOrderResponse.builder()
                .totalNum(0L)
                .issuedCouponInfoList(null)
                .build();
    }
}
