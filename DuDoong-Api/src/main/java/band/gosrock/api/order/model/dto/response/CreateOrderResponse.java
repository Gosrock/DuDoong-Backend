package band.gosrock.api.order.model.dto.response;


import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.user.domain.Profile;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateOrderResponse {
    private final String orderId;
    private final String orderName;
    private final String customerEmail;
    private final String customerName;
    private final Long amount;

    public static CreateOrderResponse from(Order order, Profile profile) {
        return CreateOrderResponse.builder()
                .customerEmail(profile.getEmail())
                .customerName(profile.getName())
                .orderName(order.getOrderName())
                .orderId(order.getUuid())
                .amount(order.getTotalPaymentPrice().longValue())
                .build();
    }
}
