package band.gosrock.domain.domains.order.domain;


import band.gosrock.domain.common.vo.Money;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentInfo {

    // 총 결제금액
    @Embedded private Money paymentAmount;

    // 공급가액
    @Embedded private Money supplyAmount;

    // 총 할인 금액
    @Embedded private Money discountAmount;
}
