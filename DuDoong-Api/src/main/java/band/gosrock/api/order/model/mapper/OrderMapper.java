package band.gosrock.api.order.model.mapper;


import band.gosrock.api.order.model.dto.response.CreateOrderResponse;
import band.gosrock.api.order.model.dto.response.OrderAdminTableElement;
import band.gosrock.api.order.model.dto.response.OrderBriefElement;
import band.gosrock.api.order.model.dto.response.OrderLineTicketResponse;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.api.order.model.dto.response.OrderTicketResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTickets;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import band.gosrock.domain.domains.order.domain.OrderOptionAnswer;
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.Option;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class OrderMapper {
    private final OrderAdaptor orderAdaptor;
    private final UserAdaptor userAdaptor;
    private final IssuedTicketAdaptor issuedTicketAdaptor;
    private final TicketItemAdaptor ticketItemAdaptor;
    private final OptionAdaptor optionAdaptor;
    private final EventAdaptor eventAdaptor;

    @Transactional(readOnly = true)
    public OrderResponse toOrderResponse(String orderUuid) {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);

        Event event = getEvent(order);

        List<OrderLineTicketResponse> orderLineTicketResponses = getOrderLineTicketResponses(order);

        return OrderResponse.of(order, event, orderLineTicketResponses);
    }

    @Transactional(readOnly = true)
    public OrderResponse toOrderResponse(Order order) {
        Event event = getEvent(order);
        List<OrderLineTicketResponse> orderLineTicketResponses = getOrderLineTicketResponses(order);

        return OrderResponse.of(order, event, orderLineTicketResponses);
    }

    @Transactional(readOnly = true)
    public CreateOrderResponse toCreateOrderResponse(String orderUuid) {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        User user = userAdaptor.queryUser(order.getUserId());
        TicketItem item = ticketItemAdaptor.queryTicketItem(order.getItemId());
        return CreateOrderResponse.from(order, item, user.getProfile());
    }

    private List<OrderLineTicketResponse> getOrderLineTicketResponses(Order order) {
        User user = userAdaptor.queryUser(order.getUserId());

        return order.getOrderLineItems().stream()
                .map(
                        orderLineItem ->
                                OrderLineTicketResponse.of(
                                        order,
                                        orderLineItem,
                                        getOptionAnswerVos(orderLineItem),
                                        user.getProfile().getName(),
                                        getTicketNoName(orderLineItem.getId())))
                .toList();
    }

    private List<OptionAnswerVo> getOptionAnswerVos(OrderLineItem orderLineItem) {
        // TODO  : options 일급 컬렉션으로 리팩터링
        List<Option> options = optionAdaptor.findAllByIds(orderLineItem.getAnswerOptionIds());

        return orderLineItem.getOrderOptionAnswers().stream()
                .map(
                        orderOptionAnswer ->
                                orderOptionAnswer.getOptionAnswerVo(
                                        getOption(options, orderOptionAnswer)))
                .toList();
    }

    private Option getOption(List<Option> options, OrderOptionAnswer orderOptionAnswer) {
        return options.stream()
                .filter(option -> option.getId().equals(orderOptionAnswer.getOptionId()))
                .findFirst()
                .orElseThrow();
    }

    private String getTicketNoName(Long orderLineItemId) {
        return issuedTicketAdaptor.findOrderLineIssuedTickets(orderLineItemId).getTicketNoName();
    }

    public OrderBriefElement toOrderBriefElement(Order order) {
        IssuedTickets orderIssuedTickets =
                issuedTicketAdaptor.findOrderIssuedTickets(order.getUuid());
        return OrderBriefElement.of(order, getEvent(order), orderIssuedTickets);
    }

    public Slice<OrderBriefElement> toOrderBriefsResponse(Slice<Order> ordersWithPagination) {
        return ordersWithPagination.map(this::toOrderBriefElement);
    }

    public Page<OrderAdminTableElement> toOrderAdminTableElement(Long eventId, Page<Order> orders) {
        List<Long> userIds = orders.stream().map(Order::getUserId).distinct().toList();
        List<User> users = userAdaptor.findUserByIdIn(userIds);
        Event event = eventAdaptor.findById(eventId);
        return orders.map(
                order -> {
                    User user =
                            users.stream()
                                    .filter(u -> u.getId().equals(order.getUserId()))
                                    .findFirst()
                                    .orElseThrow();
                    return OrderAdminTableElement.of(order, event, user);
                });
    }

    private Event getEvent(Order order) {
        return eventAdaptor.findById(order.getItemGroupId());
    }

    public OrderTicketResponse toOrderTicketResponse(Order order) {
        IssuedTickets orderIssuedTickets =
                issuedTicketAdaptor.findOrderIssuedTickets(order.getUuid());
        Event event = getEvent(order);
        List<IssuedTicketInfoVo> issuedTicketInfoVos = orderIssuedTickets.getIssuedTicketInfoVos();
        return OrderTicketResponse.of(order, event, issuedTicketInfoVos);
    }
}
