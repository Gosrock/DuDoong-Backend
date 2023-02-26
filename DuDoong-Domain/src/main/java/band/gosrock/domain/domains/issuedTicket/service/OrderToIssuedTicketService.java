package band.gosrock.domain.domains.issuedTicket.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.stream.LongStream;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class OrderToIssuedTicketService {

    private final UserAdaptor userAdaptor;
    private final OrderAdaptor orderAdaptor;
    //    private final OptionAdaptor optionAdaptor;

    public List<IssuedTicket> execute(TicketItem ticketItem, String orderUuid, Long userId) {
        User user = userAdaptor.queryUser(userId);
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        Long eventId = ticketItem.getEventId();
        return order.getOrderLineItems().stream()
                .map(
                        orderLineItem ->
                                orderLineToIssuedTickets(
                                        ticketItem, user, order, eventId, orderLineItem))
                .flatMap(Collection::stream)
                .toList();
    }

    private List<IssuedTicket> orderLineToIssuedTickets(
            TicketItem ticketItem,
            User user,
            Order order,
            Long eventId,
            OrderLineItem orderLineItem) {
        Long quantity = orderLineItem.getQuantity();
        return LongStream.range(0, quantity)
                .mapToObj(i -> IssuedTicket.create(ticketItem, user, order, eventId, orderLineItem))
                .toList();
    }
}
