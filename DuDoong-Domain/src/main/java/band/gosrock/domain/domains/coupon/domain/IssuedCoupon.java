package band.gosrock.domain.domains.coupon.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Builder
    public IssuedCoupon(CouponCampaign couponCampaign, Long userId) {
        this.couponCampaign = couponCampaign;
        this.userId = userId;
        this.usageStatus = false;
    }
}
