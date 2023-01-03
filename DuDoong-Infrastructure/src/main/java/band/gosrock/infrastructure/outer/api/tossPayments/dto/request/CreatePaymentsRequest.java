package band.gosrock.infrastructure.outer.api.tossPayments.dto.request;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreatePaymentsRequest {
    private String method;
    private Long amount;
    private String orderId;
    private String orderName;
    private String successUrl;
    private String failUrl;
}
