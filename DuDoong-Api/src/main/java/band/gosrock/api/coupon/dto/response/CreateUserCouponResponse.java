package band.gosrock.api.coupon.dto.response;


import band.gosrock.common.annotation.DateFormat;
import band.gosrock.domain.domains.coupon.domain.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateUserCouponResponse {

    @Schema(description = "발급한 쿠폰 id")
    private final Long issuedCouponId;

    @Schema(description = "쿠폰 캠페인 id")
    private final Long couponCampaignId;

    @Schema(description = "쿠폰 코드")
    private final String couponCode;

    // TODO : DateFormat 어노테이션 적용
    @Schema(type = "string", pattern = "yyyy-MM-dd HH:mm", description = "쿠폰 유효 기간")
    @DateFormat
    private final LocalDateTime validTerm;

    @Schema(description = "할인타입(정액,정률)")
    private final DiscountType discountType;

    @Schema(description = "할인량")
    private final Long discountAmount;
}
