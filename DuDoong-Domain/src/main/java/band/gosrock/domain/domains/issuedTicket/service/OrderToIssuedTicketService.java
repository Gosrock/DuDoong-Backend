package band.gosrock.domain.domains.issuedTicket.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketItemInfoVo;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketUserInfoVo;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import band.gosrock.domain.domains.order.domain.OrderOptionAnswer;
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
        Long eventId = ticketItem.getEvent().getId();
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
                .mapToObj(i -> createIssuedTicket(ticketItem, user, order, eventId, orderLineItem))
                .toList();
    }

    private IssuedTicket createIssuedTicket(
            TicketItem ticketItem,
            User user,
            Order order,
            Long eventId,
            OrderLineItem orderLineItem) {
        return IssuedTicket.builder()
                .issuedTicketOptionAnswers(getIssuedTicketOptionAnswers(orderLineItem))
                .itemInfo(IssuedTicketItemInfoVo.from(ticketItem))
                .price(ticketItem.getPrice())
                .orderLineId(orderLineItem.getId())
                .orderUuid(order.getUuid())
                .issuedTicketStatus(IssuedTicketStatus.ENTRANCE_INCOMPLETE)
                .userInfo(IssuedTicketUserInfoVo.from(user))
                .eventId(eventId)
                .build();
    }

    //    private static List<Long> getOptionIds(List<OrderOptionAnswer> orderOptionAnswers) {
    //        return orderOptionAnswers.stream().map(OrderOptionAnswer::getOptionId).toList();
    //    }

    private List<IssuedTicketOptionAnswer> getIssuedTicketOptionAnswers(
            OrderLineItem orderLineItem) {
        List<OrderOptionAnswer> orderOptionAnswers = orderLineItem.getOrderOptionAnswers();
        //        List<Option> options = getOptionsFromOptionAnswers(orderOptionAnswers);

        return orderOptionAnswers.stream().map(IssuedTicketOptionAnswer::from).toList();
    }

    //    private List<Option> getOptionsFromOptionAnswers(List<OrderOptionAnswer>
    // orderOptionAnswers) {
    //        return optionAdaptor.findAllByIds(getOptionIds(orderOptionAnswers));
    //    }
    //
    //    private Option findOptionFromAnswers(List<Option> options, Long optionId) {
    //        return options.stream()
    //                .filter(option -> option.getId().equals(optionId))
    //                .findFirst()
    //                .orElseThrow();
    //    }
}
