package band.gosrock.domain.domains.order.domain;


import band.gosrock.domain.common.vo.Money;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentInfo {

    // 총 결제금액
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "paymentAmount"))
    private Money paymentAmount;

    // 공급가액

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "supplyAmount"))
    private Money supplyAmount;
    // 총 할인 금액
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "discountAmount"))
    private Money discountAmount;

    @Builder
    public PaymentInfo(Money paymentAmount, Money supplyAmount, Money discountAmount) {
        this.paymentAmount = paymentAmount;
        this.supplyAmount = supplyAmount;
        this.discountAmount = discountAmount;
    }
}
