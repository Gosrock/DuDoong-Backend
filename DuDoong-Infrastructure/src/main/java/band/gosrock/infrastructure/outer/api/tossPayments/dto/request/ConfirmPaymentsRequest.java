package band.gosrock.infrastructure.outer.api.tossPayments.dto.request;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConfirmPaymentsRequest {
    private final String paymentKey;
    private final String orderId;
    private final Long amount;
}
