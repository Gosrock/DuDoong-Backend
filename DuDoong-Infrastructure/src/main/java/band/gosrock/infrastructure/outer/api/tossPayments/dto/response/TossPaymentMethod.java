package band.gosrock.infrastructure.outer.api.tossPayments.dto.response;


import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsEnumNotMatchException;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TossPaymentMethod {
    CARD("카드"),
    VIRTUAL_ACCOUNT("가상계좌"),
    EASYPAY("간편결제"),
    MOBILE_PAY("휴대폰"),
    BANK_TRANSFER("계좌이체"),
    GIFT_CARD("상품권");

    private String kr;

    @JsonCreator
    static TossPaymentMethod findValue(String code) {
        return Arrays.stream(TossPaymentMethod.values())
                .filter(PaymentMethod -> PaymentMethod.getKr().equals(code))
                .findFirst()
                .orElseThrow(() -> PaymentsEnumNotMatchException.EXCEPTION);
    }
}
