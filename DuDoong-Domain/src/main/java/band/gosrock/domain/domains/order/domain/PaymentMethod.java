package band.gosrock.domain.domains.order.domain;


import band.gosrock.domain.domains.order.exception.NotSupportedOrderMethodException;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.TossPaymentMethod;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentMethod {
    // 간편결제
    EASYPAY("EASYPAY","간편 결제"),
    // 카드결제
    CARD("CARD","카드 결제"),
    // 승인결제
    APPROVAL("APPROVAL","승인 결제"),
    // 결제방식 미지정상태
    DEFAULT("DEFAULT","결제 방식 미지정");
    private String value;

    @JsonValue
    private String kr;

    public static PaymentMethod from(TossPaymentMethod tossPaymentMethod) {
        try {
            return PaymentMethod.valueOf(tossPaymentMethod.name());
        } catch (IllegalArgumentException illegalArgumentException) {
            throw NotSupportedOrderMethodException.EXCEPTION;
        }
    }
}
