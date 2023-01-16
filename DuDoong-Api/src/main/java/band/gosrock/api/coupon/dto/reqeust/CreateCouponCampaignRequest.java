package band.gosrock.api.coupon.dto.reqeust;


import band.gosrock.domain.domains.coupon.domain.ApplyTarget;
import band.gosrock.domain.domains.coupon.domain.CouponCampaign;
import band.gosrock.domain.domains.coupon.domain.CouponStockInfo;
import band.gosrock.domain.domains.coupon.domain.DiscountType;
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

    @NotNull(message = "CREATE_COUPON_CAMPAIGN_REQUEST_EMPTY_HOST_ID")
    private Long hostId;

    @NotNull(message = "CREATE_COUPON_CAMPAIGN_REQUEST_EMPTY_DISCOUNT_TYPE")
    private DiscountType discountType;

    @Nullable
    @Schema(nullable = true, defaultValue = "ALL")
    private ApplyTarget applyTarget;

    @NotNull(message = "CREATE_COUPON_CAMPAIGN_REQUEST_EMPTY_VALID_TERM")
    @Positive(message = "CREATE_COUPON_CAMPAIGN_REQUEST_NOT_POSITIVE_VALID_TERM")
    private Long validTerm;

    // 쿠폰 발행 시작 시각
    @NotNull(message = "CREATE_COUPON_CAMPAIGN_REQUEST_EMPTY_START_AT")
    private LocalDateTime startAt;

    // 쿠폰 발행 마감 시각
    @NotNull(message = "CREATE_COUPON_CAMPAIGN_REQUEST_EMPTY_END_AT")
    @Future(message = "CREATE_COUPON_CAMPAIGN_REQUEST_END_AT_MUST_BE_FUTURE")
    private LocalDateTime endAt;

    @NotNull(message = "CREATE_COUPON_CAMPAIGN_REQUEST_EMPTY_ISSUED_AMOUNT")
    @Positive(message = "CREATE_COUPON_CAMPAIGN_REQUEST_NOT_POSITIVE_ISSUED_AMOUNT")
    private Long issuedAmount;

    @NotNull(message = "CREATE_COUPON_CAMPAIGN_REQUEST_EMPTY_DISCOUNT_AMOUNT")
    @Positive(message = "CREATE_COUPON_CAMPAIGN_REQUEST_NOT_POSITIVE_DISCOUNT_AMOUNT")
    private Long discountAmount;

    @NotBlank(message = "CREATE_COUPON_CAMPAIGN_REQUEST_4008")
    private String couponCode;

    public CouponCampaign toOnceEntity() {
        CouponStockInfo couponStockInfo =
                CouponStockInfo.builder()
                        .issuedAmount(issuedAmount)
                        .remainingAmount(issuedAmount)
                        .build();

        return CouponCampaign.builder()
                .hostId(hostId)
                .discountType(discountType)
                .applyTarget(applyTarget)
                .validTerm(validTerm)
                .startAt(startAt)
                .endAt(endAt)
                .couponStockInfo(couponStockInfo)
                .discountAmount(discountAmount)
                .couponCode(couponCode)
                .build();
    }
}
