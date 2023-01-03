package band.gosrock.infrastructure.outer.api.tossPayments.dto.request;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ConfirmPaymentsRequest {
    private final String paymentKey;
    private final String orderId;
    private final Long amount;
}
