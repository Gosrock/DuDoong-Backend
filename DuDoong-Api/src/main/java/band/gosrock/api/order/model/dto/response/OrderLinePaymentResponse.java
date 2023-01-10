package band.gosrock.api.order.model.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderLinePaymentResponse {
    private final String paymentMethod;
    private final String supplyAmount;
    private final String discountAmount;
    private final String couponName = "사용하지 않음";
    private final String totalAmount;
}
