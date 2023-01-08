package band.gosrock.domain.domains.coupon.domain;


import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_issued_coupon")
public class IssuedCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issued_coupon_id")
    private Long id;

    private Long couponCampaignId;

    private Long userId;

    private Boolean usageStatus;

    @Builder
    public IssuedCoupon(Long couponCampaignId, Long userId) {
        this.couponCampaignId = couponCampaignId;
        this.userId = userId;
        this.usageStatus = false;
    }
}
