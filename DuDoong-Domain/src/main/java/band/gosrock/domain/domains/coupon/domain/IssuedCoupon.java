package band.gosrock.domain.domains.coupon.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.Money;
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

    private Boolean usageStatus;

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
        if (supply.isGreaterThanOrEqual(Money.wons(discount))) {
            return Money.wons(discount);
        }
        return Money.ZERO;
    }

    @Builder
    public IssuedCoupon(CouponCampaign couponCampaign, Long userId) {
        this.couponCampaign = couponCampaign;
        this.userId = userId;
        this.usageStatus = false;
    }

    public String getCouponName() { //쿠폰코드==쿠폰이름
        return this.couponCampaign.getCouponCode();
    }
}
