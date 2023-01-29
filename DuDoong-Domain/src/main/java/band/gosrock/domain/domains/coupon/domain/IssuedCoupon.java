package band.gosrock.domain.domains.coupon.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.coupon.exception.AlreadyRecoveredCouponException;
import band.gosrock.domain.domains.coupon.exception.AlreadyUsedCouponException;
import band.gosrock.domain.domains.coupon.exception.NotApplicableCouponException;
import band.gosrock.domain.domains.coupon.exception.NotMyCouponException;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity(name = "tbl_issued_coupon")
public class IssuedCoupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issued_coupon_id")
    private Long id;

    private Long userId;

    private Boolean usageStatus = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_campaign_id", nullable = false)
    private CouponCampaign couponCampaign;
    /** 주문에서 사용하는 할인 금액 계산 함수, 할인 금액보다 결제 금액이 작을 경우 할인 불가로 Money.ZERO 리턴 */
    public Money getDiscountAmount(Money supplyAmount) {
        if (couponCampaign.getDiscountType().equals(DiscountType.AMOUNT)) { // 정액 할인
            return checkSupplyIsGreaterThenDiscount(
                    supplyAmount, couponCampaign.getDiscountAmount());
        }
        // 정률 할인
        Long discountAmount =
                supplyAmount.getDiscountAmountByPercentage(
                        supplyAmount, couponCampaign.getDiscountAmount());
        return checkSupplyIsGreaterThenDiscount(supplyAmount, discountAmount);
    }

    public Money checkSupplyIsGreaterThenDiscount(Money supply, Long discount) {
        if (supply.isLessThan(Money.wons(discount))) {
            throw NotApplicableCouponException.EXCEPTION;
        }
        return Money.wons(discount);
    }

    @Builder
    public IssuedCoupon(CouponCampaign couponCampaign, Long userId) {
        this.couponCampaign = couponCampaign;
        this.userId = userId;
        this.usageStatus = false;
    }

    public String getCouponName() { // 쿠폰코드==쿠폰이름
        return this.couponCampaign.getCouponCode();
    }

    public void validMine(Long userId) {
        if (!Objects.equals(userId, this.userId)) {
            throw NotMyCouponException.EXCEPTION;
        }
    }

    public void use() {
        if (usageStatus) { // 동시성 이슈 가능
            throw AlreadyUsedCouponException.EXCEPTION;
        }
        usageStatus = true;
    }

    public void recovery() {
        if (!usageStatus) { // 동시성 이슈 가능
            throw AlreadyRecoveredCouponException.EXCEPTION;
        }
        usageStatus = false;
    }

    public Boolean isAvailableTerm() {
        return !LocalDateTime.now()
                .isAfter(this.getCreatedAt().plusDays(this.getCouponCampaign().getValidTerm()));
    }

    public LocalDateTime calculateValidTerm() {
        return this.getCreatedAt().plusDays(this.getCouponCampaign().getValidTerm());
    }
}
