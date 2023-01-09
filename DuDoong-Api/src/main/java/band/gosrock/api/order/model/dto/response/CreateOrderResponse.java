package band.gosrock.api.order.model.dto.response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateOrderResponse {
    private final String orderId;
    private final String orderName;
    private final String customerEmail;
    private final String customerName;
}
