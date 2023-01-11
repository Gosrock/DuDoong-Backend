package band.gosrock.api.order.model.dto.response;


import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.user.domain.Profile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateOrderResponse {
    @Schema(description = "UUId")
    private final String orderId;
    @Schema(description = "상품명")
    private final String orderName;
    @Schema(description = "고객이메일")
    private final String customerEmail;
    @Schema(description = "고객이름")
    private final String customerName;
    @Schema(description = "결제금액")
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
