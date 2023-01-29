package band.gosrock.api.order.model.mapper;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.order.model.dto.response.CreateOrderResponse;
import band.gosrock.api.order.model.dto.response.OrderLineTicketResponse;
import band.gosrock.api.order.model.dto.response.OrderResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.common.vo.OptionAnswerVo;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import band.gosrock.domain.domains.order.domain.OrderOptionAnswer;
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.Option;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.user.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class OrderMapper {
    private final OrderAdaptor orderAdaptor;
    private final UserUtils userUtils;
    private final IssuedTicketAdaptor issuedTicketAdaptor;
    private final TicketItemAdaptor ticketItemAdaptor;
    private final OptionAdaptor optionAdaptor;
    private final EventAdaptor eventAdaptor;

    @Transactional(readOnly = true)
    public OrderResponse toOrderResponse(String orderUuid) {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);

        Event event = eventAdaptor.findById(order.getItemGroupId());

        List<OrderLineTicketResponse> orderLineTicketResponses = getOrderLineTicketResponses(order);

        return OrderResponse.of(order, event.getRefundInfoVo(), orderLineTicketResponses);
    }

    @Transactional(readOnly = true)
    public CreateOrderResponse toCreateOrderResponse(String orderUuid) {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        User user = userUtils.getCurrentUser();
        TicketItem item = ticketItemAdaptor.queryTicketItem(order.getItemId());
        return CreateOrderResponse.from(order, item, user.getProfile());
    }

    @NotNull
    private List<OrderLineTicketResponse> getOrderLineTicketResponses(Order order) {
        return order.getOrderLineItems().stream()
                .map(
                        orderLineItem ->
                                OrderLineTicketResponse.of(
                                        order,
                                        orderLineItem,
                                        getOptionAnswerVos(orderLineItem),
                                        getUserName(),
                                        getTicketNoName(orderLineItem.getId())))
                .toList();
    }

    private String getUserName() {
        User user = userUtils.getCurrentUser();
        return user.getProfile().getName();
    }

    private List<Long> getOptionIds(OrderLineItem orderLineItem) {
        return orderLineItem.getOrderOptionAnswers().stream()
                .map(OrderOptionAnswer::getOptionId)
                .toList();
    }

    private List<OptionAnswerVo> getOptionAnswerVos(OrderLineItem orderLineItem) {
        List<Long> optionIds = getOptionIds(orderLineItem);
        List<Option> options = optionAdaptor.findAllByIds(optionIds);

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
        List<String> issuedTicketNos = getIssuedTicketNos(orderLineItemId);
        Integer size = issuedTicketNos.size();
        // 없을 경우긴 함 테스트를 위해서
        if (issuedTicketNos.isEmpty()) return "";
        else if (size.equals(1)) return String.format("%s (%d매)", issuedTicketNos.get(0), size);
        else
            return String.format(
                    "%s ~ %s (%d매)", issuedTicketNos.get(0), issuedTicketNos.get(size - 1), size);
    }

    private List<String> getIssuedTicketNos(Long orderLineItemId) {
        return issuedTicketAdaptor.findAllByOrderLineId(orderLineItemId).stream()
                .map(IssuedTicket::getIssuedTicketNo)
                .toList();
    }
}
