package band.gosrock.domain.domains.coupon.domain;


import band.gosrock.domain.common.model.BaseTimeEntity;
import band.gosrock.domain.common.vo.DateTimePeriod;
import band.gosrock.domain.domains.coupon.exception.WrongDiscountAmountException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
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
    @ColumnDefault("'ALL'")
    private ApplyTarget applyTarget;

    // 사용기한(일자) ex.10 -> 발급 이후 10일 동안 쿠폰 사용 가능
    private Long validTerm;

    // 쿠폰 발행 가능 기간
    @Embedded private DateTimePeriod dateTimePeriod;

    // 쿠폰 총 발행량 및 잔량
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
            DateTimePeriod dateTimePeriod,
            CouponStockInfo couponStockInfo,
            Long discountAmount,
            String couponCode) {
        this.hostId = hostId;
        this.discountType = discountType;
        this.applyTarget = applyTarget;
        this.validTerm = validTerm;
        this.dateTimePeriod = dateTimePeriod;
        this.couponStockInfo = couponStockInfo;
        this.discountAmount = discountAmount;
        this.couponCode = couponCode;
    }

    // 정률 할인시 discountAmount 값이 100 이하인지 검증
    public void validatePercentageAmount(DiscountType discountType, Long DiscountAmount) {
        if (discountType.equals(DiscountType.PERCENTAGE) && DiscountAmount > 100) {
            throw WrongDiscountAmountException.EXCEPTION;
        }
    }

    public void decreaseCouponStock() {
        couponStockInfo.decreaseCouponStock();
    }
}
