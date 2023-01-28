package band.gosrock.api.coupon.dto.response;


import band.gosrock.domain.common.vo.IssuedCouponInfoVo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReadIssuedCouponResponse {

    @Schema(description = "사용 가능 쿠폰 총 개수")
    private final Long availableCouponNum;

    @Schema(description = "사용 가능 쿠폰 정보 리스트")
    private final List<IssuedCouponInfoVo> availableCouponInfoList;

    @Schema(description = "사용됨/기한만료 쿠폰 총 개수")
    private final Long expiredCouponNum;

    @Schema(description = "사용됨/기한만료 쿠폰 정보 리스트")
    private final List<IssuedCouponInfoVo> expiredCouponInfoList;
}
