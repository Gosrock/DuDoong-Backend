package band.gosrock.domain.domains.coupon.domain;


import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponStockInfo {

    private Long issuedAmount;
    private Long remainingAmount;

    @Builder
    public CouponStockInfo(Long issuedAmount, Long remainingAmount) {
        this.issuedAmount = issuedAmount;
        this.remainingAmount = remainingAmount;
    }
}
