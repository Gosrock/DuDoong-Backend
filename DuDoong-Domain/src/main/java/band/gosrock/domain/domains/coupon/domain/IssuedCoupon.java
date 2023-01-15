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

    public Money getDiscountAmount(Money supplyAmount) {
        if (couponCampaign.getDiscountType().equals(DiscountType.AMOUNT)) {
            // 그냥 이정도로만 만들게유!! 메소드만 필요해서 - 찬진
            // TODO : 할인 금액이 결제 가능 금액보다 작을때 에러 등 검증 필요
            return supplyAmount.minus(Money.wons(couponCampaign.getDiscountAmount()));
        }
        return Money.ZERO;
    }

    @Builder
    public IssuedCoupon(CouponCampaign couponCampaign, Long userId) {
        this.couponCampaign = couponCampaign;
        this.userId = userId;
        this.usageStatus = false;
    }

    public String getCouponName() {
        // 쿠폰코드==쿠폰이름
        return this.couponCampaign.getCouponCode();
    }
}
