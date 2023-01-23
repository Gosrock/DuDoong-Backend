package band.gosrock.api.coupon.dto.reqeust;


import band.gosrock.common.annotation.DateFormat;
import band.gosrock.domain.common.vo.DateTimePeriod;
import band.gosrock.domain.domains.coupon.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

@Getter
@RequiredArgsConstructor
public class CreateCouponCampaignRequest {

    @NotNull(message = "host Id를 입력해주세요.")
    private Long hostId;

    @NotNull(message = "discountType을 입력해주세요.")
    private DiscountType discountType;

    @Nullable
    @Schema(nullable = true, defaultValue = "ALL")
    private ApplyTarget applyTarget;

    @NotNull(message = "validTerm을 입력해주세요.")
    @Positive(message = "validTerm은 양수여야합니다.(유효기간)")
    private Long validTerm;

    // 쿠폰 발행 시작 시각
    @Schema(type = "string", pattern = "yyyy-MM-dd HH:mm", description = "쿠폰 발행 시작 시간")
    @NotNull(message = "startAt을 입력해주세요.")
    @DateFormat
    private LocalDateTime startAt;

    // 쿠폰 발행 마감 시각
    @Schema(type = "string", pattern = "yyyy-MM-dd HH:mm", description = "쿠폰 발행 마감 시간")
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

    public CouponCampaign toOnceEntity() {
        CouponStockInfo couponStockInfo =
                CouponStockInfo.builder()
                        .issuedAmount(issuedAmount)
                        .remainingAmount(issuedAmount)
                        .build();
        DateTimePeriod dateTimePeriod =
                DateTimePeriod.builder().startAt(startAt).endAt(endAt).build();

        return CouponCampaign.builder()
                .hostId(hostId)
                .discountType(discountType)
                .applyTarget(applyTarget)
                .validTerm(validTerm)
                .dateTimePeriod(dateTimePeriod)
                .couponStockInfo(couponStockInfo)
                .discountAmount(discountAmount)
                .couponCode(couponCode)
                .build();
    }
}
