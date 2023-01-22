package band.gosrock.domain.domains.coupon.domain;


import band.gosrock.domain.domains.coupon.exception.NoCouponStockLeftException;
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

    public void checkCouponLeft() {
        if (remainingAmount < 1) { // 재고 없을 경우 에러 처리
            throw NoCouponStockLeftException.EXCEPTION;
        }
    }

    public void decreaseCouponStock() {
        checkCouponLeft();
        this.remainingAmount = remainingAmount - 1;
    }
}
