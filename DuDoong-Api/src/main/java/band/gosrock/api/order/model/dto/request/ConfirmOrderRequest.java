package band.gosrock.api.order.model.dto.request;


import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.ConfirmPaymentsRequest;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConfirmOrderRequest {
    private final String paymentKey;
    private final String orderId;
    private final Long amount;

    public ConfirmPaymentsRequest toConfirmPaymentsRequest() {
        return ConfirmPaymentsRequest.builder()
                .orderId(orderId)
                .amount(amount)
                .paymentKey(paymentKey)
                .build();
    }
}
