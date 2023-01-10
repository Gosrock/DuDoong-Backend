package band.gosrock.api.order.model.dto.response;


import band.gosrock.domain.common.vo.RefundInfoVo;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.user.domain.Profile;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderResponse {

    private final List<OrderLineResponse> orderLines;

    public static OrderResponse from(Order order, Profile profile) {
        return OrderResponse.builder()
                .customerEmail(profile.getEmail())
                .customerName(profile.getName())
                .orderName(order.getOrderName())
                .orderId(order.getUuid())
                .amount(order.getTotalPaymentPrice().longValue())
                .build();
    }
}
