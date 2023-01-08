package band.gosrock.domain.domains.coupon.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "tbl_coupon_campaign")
public class CouponCampaign extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_campaign_id")
    private Long id;

    private Long hostId;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Enumerated(EnumType.STRING)
    private ApplyTarget applyTarget;

    private Long validTerm;

    // 쿠폰 발행 시작 시각
    private LocalDateTime startAt;
    // 쿠폰 발행 마감 시각
    private LocalDateTime endAt;

    @Embedded private CouponStockInfo couponStockInfo;

    private Long discountAmount;

    private String couponCode;

    @OneToMany(mappedBy = "couponCampaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<IssuedCoupon> issuedCoupons = new ArrayList<>();

    @Builder
    public CouponCampaign(
            Long hostId,
            DiscountType discountType,
            ApplyTarget applyTarget,
            Long validTerm,
            LocalDateTime startAt,
            LocalDateTime endAt,
            CouponStockInfo couponStockInfo,
            Long discountAmount,
            String couponCode) {
        this.hostId = hostId;
        this.discountType = discountType;
        this.applyTarget = applyTarget;
        this.validTerm = validTerm;
        this.startAt = startAt;
        this.endAt = endAt;
        this.couponStockInfo = couponStockInfo;
        this.discountAmount = discountAmount;
        this.couponCode = couponCode;
    }
}
