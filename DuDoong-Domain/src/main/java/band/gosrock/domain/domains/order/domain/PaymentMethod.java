package band.gosrock.domain.domains.order.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentMethod {
    // 간편결제
    EASYPAY("EASYPAY"),
    // 카드결제
    CARD("CARD"),
    // 승인결제
    APPROVAL("APPROVAL");
    private String value;
}
