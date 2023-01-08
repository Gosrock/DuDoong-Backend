package band.gosrock.infrastructure.outer.api.tossPayments.dto.request;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatePaymentsRequest {
    private final String method;
    private final Long amount;
    private final String orderId;
    private final String orderName;
    private final String successUrl;
    private final String failUrl;
}
