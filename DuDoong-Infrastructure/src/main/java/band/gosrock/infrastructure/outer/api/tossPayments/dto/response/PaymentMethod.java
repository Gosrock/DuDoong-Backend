package band.gosrock.infrastructure.outer.api.tossPayments.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentMethod {
    CARD("카드"),
    VIRTUAL_ACCOUNT("가상계좌"),
    EASY_PAY("간편결제"),
    MOBILE_PAY("휴대폰"),
    BANK_TRANSFER("계좌이체"),
    GIFT_CARD("상품권");

    private String kr;
}
