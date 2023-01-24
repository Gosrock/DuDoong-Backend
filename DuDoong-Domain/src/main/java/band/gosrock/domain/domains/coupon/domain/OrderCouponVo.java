package band.gosrock.domain.domains.coupon.domain;


import band.gosrock.domain.common.vo.Money;
import javax.persistence.Embeddable;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
public class OrderCouponVo {
    private String name = "사용하지 않음";
    private Money discountAmount = Money.ZERO;
    private Long couponId = 0L;

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

    public static OrderCouponVo empty() {
        return new OrderCouponVo();
    }
}
