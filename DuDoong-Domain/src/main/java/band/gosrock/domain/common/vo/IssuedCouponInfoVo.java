package band.gosrock.domain.common.vo;


import band.gosrock.common.annotation.DateFormat;
import band.gosrock.domain.domains.coupon.domain.ApplyTarget;
import band.gosrock.domain.domains.coupon.domain.DiscountType;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IssuedCouponInfoVo {

    private final Long issuedCouponId;

    // 사용여부
    private final boolean usageStatus;

    private final ApplyTarget applyTarget;

    private final String couponCode;

    // 정률할인,정액할인
    private final DiscountType discountType;

    private final Long discountAmount;

    // 쿠폰 사용 가능 마감 시각
    @DateFormat
    private final LocalDateTime validDateTime;

    public static IssuedCouponInfoVo of(IssuedCoupon issuedCoupon) {
        return IssuedCouponInfoVo.builder()
                .issuedCouponId(issuedCoupon.getId())
                .usageStatus(issuedCoupon.isUsageStatus())
                .applyTarget(issuedCoupon.getCouponCampaign().getApplyTarget())
                .couponCode(issuedCoupon.getCouponCampaign().getCouponCode())
                .discountType(issuedCoupon.getCouponCampaign().getDiscountType())
                .discountAmount(issuedCoupon.getCouponCampaign().getDiscountAmount())
                .validDateTime(
                        issuedCoupon
                                .getCreatedAt()
                                .plusDays(issuedCoupon.getCouponCampaign().getValidTerm()))
                .build();
    }
}
