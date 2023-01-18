package band.gosrock.api.order.model.dto.request;


import lombok.Getter;

@Getter
public class ConfirmOrderRequest {
    private String paymentKey;
    private Long amount;
}
