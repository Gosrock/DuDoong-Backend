package band.gosrock.infrastructure.outer.api.tossPayments.dto.response;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentEasyPay {
    private String provider;
    private Long amount;
    private Long discountAmount;
}
