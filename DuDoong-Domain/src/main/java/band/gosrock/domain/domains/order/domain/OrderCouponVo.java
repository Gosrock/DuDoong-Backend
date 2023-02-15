package band.gosrock.domain.domains.order.domain;

import static band.gosrock.common.consts.DuDoongStatic.MINIMUM_PAYMENT_WON;
import static band.gosrock.common.consts.DuDoongStatic.ZERO;

import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import band.gosrock.domain.domains.order.exception.LessThanMinmumPaymentOrderException;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor
public class OrderCouponVo {

    @Column(name = "coupon_name")
    private String name = "사용하지 않음";

    private Money discountAmount = Money.ZERO;
    private Long couponId = ZERO;

    @Builder
    public OrderCouponVo(String name, Money discountAmount, Long couponId) {
        this.name = name;
        this.discountAmount = discountAmount;
        this.couponId = couponId;
    }

    public static OrderCouponVo of(IssuedCoupon coupon, Money OrderSupplyAmount) {
        return OrderCouponVo.builder()
                .couponId(coupon.getId())
                .discountAmount(coupon.getDiscountAmount(OrderSupplyAmount))
                .name(coupon.getCouponName())
                .build();
    }

    public void validMinimumPaymentAmount(Money supplyAmount) {
        Money paymentAmount = supplyAmount.minus(this.discountAmount);
        // 0원 결제는 가능함!
        if (!paymentAmount.equals(Money.ZERO)
                && paymentAmount.isLessThan(Money.wons(MINIMUM_PAYMENT_WON))) {
            throw LessThanMinmumPaymentOrderException.EXCEPTION;
        }
    }

    public Boolean isDefault() {
        return couponId.equals(ZERO);
    }

    public static OrderCouponVo empty() {
        return new OrderCouponVo();
    }
}
