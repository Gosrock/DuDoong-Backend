package band.gosrock.api.order.model.mapper;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.order.model.dto.response.CreateOrderResponse;
import band.gosrock.api.order.model.dto.response.OrderLineTicketResponse;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.user.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class OrderMapper {
    private final OrderAdaptor orderAdaptor;
    private final UserUtils userUtils;
    private final IssuedTicketAdaptor issuedTicketAdaptor;
    private final TicketItemAdaptor ticketItemAdaptor;

    @Transactional(readOnly = true)
    public OrderResponse toOrderResponse(String orderUuid) {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        User user = userUtils.getCurrentUser();
        String name = user.getProfile().getName();
        List<OrderLineTicketResponse> orderLineTicketResponses =
                order.getOrderLineItems().stream()
                        .map(
                                orderLineItem ->
                                        OrderLineTicketResponse.of(
                                                order,
                                                orderLineItem,
                                                name,
                                                getTicketNoName(orderLineItem.getId())))
                        .toList();

        return OrderResponse.of(order, orderLineTicketResponses);
    }

    @Transactional(readOnly = true)
    public CreateOrderResponse toCreateOrderResponse(String orderUuid) {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        User user = userUtils.getCurrentUser();
        TicketItem item = ticketItemAdaptor.queryTicketItem(order.getItemId());
        return CreateOrderResponse.from(order, item, user.getProfile());
    }

    private String getTicketNoName(Long orderLineItemId) {
        List<String> issuedTicketNos =
                issuedTicketAdaptor.findAllByOrderLineId(orderLineItemId).stream()
                        .map(IssuedTicket::getIssuedTicketNo)
                        .toList();
        Integer size = issuedTicketNos.size();
        // 없을 경우긴 함 테스트를 위해서
        if (issuedTicketNos.isEmpty()) return "";
        else if (size.equals(1)) {
            return String.format("%s (%d매)", issuedTicketNos.get(0), size);
        } else {
            return String.format(
                    "%s ~ %s (%d매)", issuedTicketNos.get(0), issuedTicketNos.get(size - 1), size);
        }
    }
}
