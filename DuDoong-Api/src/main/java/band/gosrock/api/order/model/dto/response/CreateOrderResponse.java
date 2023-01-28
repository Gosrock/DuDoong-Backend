package band.gosrock.api.order.model.dto.response;


import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderMethod;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.domain.TicketType;
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
    private final Money amount;

    @Schema(description = "결제가 필요한지에 대한 여부를 결정합니다. 필요한 true면 결제창 띄우시면됩니다.", defaultValue = "true")
    private final Boolean isNeedPayment;

    @Schema(description = "주문 방식 ( 결제 방식 , 승인 방식 )")
    private final OrderMethod orderMethod;

    @Schema(description = "티켓의 타입. 승인 , 선착순 두가지입니다.")
    private final TicketType ticketType;

    public static CreateOrderResponse from(Order order, TicketItem item, Profile profile) {
        return CreateOrderResponse.builder()
                .customerEmail(profile.getEmail())
                .customerName(profile.getName())
                .orderName(order.getOrderName())
                .orderId(order.getUuid())
                .amount(order.getTotalPaymentPrice())
                .orderMethod(order.getOrderMethod())
                .isNeedPayment(order.isNeedPaid())
                .ticketType(item.getType())
                .build();
    }
}
