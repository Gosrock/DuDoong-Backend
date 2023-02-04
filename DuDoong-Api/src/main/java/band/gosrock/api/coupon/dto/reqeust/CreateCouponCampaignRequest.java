package band.gosrock.api.coupon.dto.reqeust;


import band.gosrock.common.annotation.DateFormat;
import band.gosrock.domain.domains.coupon.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import javax.validation.constraints.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

@Getter
@RequiredArgsConstructor
public class CreateCouponCampaignRequest {

    @NotNull(message = "discountType을 입력해주세요.")
    private DiscountType discountType;

    @Nullable
    @Schema(nullable = true, defaultValue = "ALL")
    private ApplyTarget applyTarget;

    @NotNull(message = "validTerm을 입력해주세요.")
    @Positive(message = "validTerm은 양수여야합니다.(유효기간)")
    private Long validTerm;

    // 쿠폰 발행 시작 시각
    @Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "쿠폰 발행 시작 시간")
    @NotNull(message = "startAt을 입력해주세요.")
    @DateFormat
    private LocalDateTime startAt;

    // 쿠폰 발행 마감 시각
    @Schema(type = "string", pattern = "yyyy.MM.dd HH:mm", description = "쿠폰 발행 마감 시간")
    @NotNull(message = "endAt을 입력해주세요.")
    @Future(message = "endAt은 값이 미래여야합니다.")
    @DateFormat
    private LocalDateTime endAt;

    @NotNull(message = "issuedAmount을 입력해주세요.")
    @Positive(message = "issuedAmount는 양수여야합니다.")
    private Long issuedAmount;

    @NotNull(message = "discountAmount을 입력해주세요.")
    @Positive(message = "discountAmount은 양수여야합니다.")
    private Long discountAmount;

    @NotBlank(message = "couponCode를 입력해주세요.")
    private String couponCode;

    @Schema(description = "쿠폰 사용 가능한 최소 결제 금액(원단위, 10000원 이상부터 입력 가능)")
    @NotNull(message = "minimumCost(원 단위)를 입력해주세요.")
    @Min(value = 10000, message = "10000원 이상부터 입력 가능합니다.")
    private Long minimumCost;
}
