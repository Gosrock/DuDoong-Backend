package band.gosrock.api.order.model.dto.request;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConfirmOrderRequest {
    private final String paymentKey;
    private final Long amount;
}
