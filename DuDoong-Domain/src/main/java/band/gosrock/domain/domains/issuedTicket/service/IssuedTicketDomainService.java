package band.gosrock.domain.domains.issuedTicket.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.exception.HostNotAuthEventException;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer;
import band.gosrock.domain.domains.issuedTicket.dto.request.CreateIssuedTicketDTO;
import band.gosrock.domain.domains.issuedTicket.dto.response.CreateIssuedTicketResponse;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderOptionAnswer;
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.Option;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssuedTicketDomainService {
    private final IssuedTicketAdaptor issuedTicketAdaptor;
    private final TicketItemAdaptor ticketItemAdaptor;
    private final EventAdaptor eventAdaptor;
    private final UserAdaptor userAdaptor;
    private final OrderAdaptor orderAdaptor;
    private final OptionAdaptor optionAdaptor;

    @RedissonLock(LockName = "티켓재고관리", identifier = "itemId")
    @Transactional
    public void withDrawIssuedTicket(Long itemId, List<IssuedTicket> issuedTickets) {
        issuedTickets.forEach(
                issuedTicket -> {
                    issuedTicket.getTicketItem().increaseQuantity(1L);
                    issuedTicketAdaptor.cancel(issuedTicket);
                });
    }

    @Transactional
    public IssuedTicketInfoVo processingEntranceIssuedTicket(
            Long currentUserId, Long issuedTicketId) {
        IssuedTicket issuedTicket = issuedTicketAdaptor.find(issuedTicketId);
        if (!Objects.equals(issuedTicket.getEvent().getHostId(), currentUserId)) {
            throw HostNotAuthEventException.EXCEPTION;
        }
        issuedTicket.entrance();
        return issuedTicket.toIssuedTicketInfoVo();
    }

    @RedissonLock(LockName = "티켓재고관리", identifier = "itemId")
    public void createIssuedTicket(Long itemId, String orderUuid, Long userId) {
        User user = userAdaptor.queryUser(userId);
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        TicketItem ticketItem = ticketItemAdaptor.queryTicketItem(itemId);
        Event event = ticketItem.getEvent();
        List<IssuedTicket> issuedTickets = order.getOrderLineItems().stream().map(orderLineItem -> {
            Long quantity = orderLineItem.getQuantity();
            List<OrderOptionAnswer> orderOptionAnswers = orderLineItem.getOrderOptionAnswer();
            List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers = orderOptionAnswers.stream()
                .map(orderOptionAnswer -> {
                    Long optionId = orderOptionAnswer.getOptionId();
                    Option option = optionAdaptor.queryOption(optionId);

                    return IssuedTicketOptionAnswer.of(option, orderOptionAnswer);
                }).toList();
            return LongStream.range(0,quantity)
                .mapToObj(i-> {
                    IssuedTicket build = IssuedTicket.builder()
                        .issuedTicketOptionAnswers(issuedTicketOptionAnswers)
                        .ticketItem(ticketItem)
                        .price(ticketItem.getPrice())
                        .orderLineId(orderLineItem.getId())
                        .orderUuid(order.getUuid())
                        .user(user)
                        .issuedTicketOptionAnswers(issuedTicketOptionAnswers)
                        .event(event)
                        .build();
                    return build;
                }).toList();
        }).flatMap(Collection::stream).toList();

        issuedTicketAdaptor.saveAll(issuedTickets);
    }
}
