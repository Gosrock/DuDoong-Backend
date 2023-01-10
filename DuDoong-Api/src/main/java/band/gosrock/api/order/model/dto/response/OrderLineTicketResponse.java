package band.gosrock.api.order.model.dto.response;


import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderLineTicketResponse {
    private final String ticketName;
    private final String orderNo;
    private final String ticketNos;
    private final LocalDateTime paymentAt;
    private final String userName;

    private final Long supplyAmount;
    private List<OptionAnswerVo> answers;

    public static OrderLineTicketResponse of(
            Order order, OrderLineItem orderLineItem, String userName, String ticketNos) {
        return OrderLineTicketResponse.builder()
                .answers(orderLineItem.getOptionAnswerVos())
                .orderNo(order.getOrderNo() + "-" + orderLineItem.getId())
                .ticketNos(ticketNos)
                .ticketName(orderLineItem.getProductName())
                .paymentAt(order.getApprovedAt())
                .userName(userName)
                .supplyAmount(orderLineItem.getQuantity())
                .build();
    }
}
