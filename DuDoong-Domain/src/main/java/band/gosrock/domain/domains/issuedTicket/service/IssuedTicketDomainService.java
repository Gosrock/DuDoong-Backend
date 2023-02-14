package band.gosrock.domain.domains.issuedTicket.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.validator.IssuedTicketValidator;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssuedTicketDomainService {
    private final IssuedTicketAdaptor issuedTicketAdaptor;
    private final TicketItemAdaptor ticketItemAdaptor;
    private final UserAdaptor userAdaptor;
    private final OrderAdaptor orderAdaptor;

    private final IssuedTicketValidator issuedTicketValidator;

    /*
    티켓 철회 로직
     */
    @RedissonLock(LockName = "티켓관리", identifier = "itemId")
    public void withdrawIssuedTicket(Long itemId, List<IssuedTicket> issuedTickets) {
        // itemId로 티켓 아이템 찾아서 (해당 락에선 ticketItem이 하나로 정해지기 때문에)
        TicketItem ticketItem = ticketItemAdaptor.queryTicketItem(itemId);
        issuedTickets.forEach(
                issuedTicket -> {
                    // 재고 복구하고
                    ticketItem.increaseQuantity(1L);
                    // 발급된 티켓 취소
                    issuedTicket.cancel();
                });
    }

    /*
    주문 승인 과정 중 티켓 아이템의 상태가 변해서 주문이 취소되는 경우
    이미 발급된 티켓 취소 로직
     */
    @RedissonLock(LockName = "티켓관리", identifier = "itemId")
    public void doneOrderEventAfterRollBackWithdrawIssuedTickets(Long itemId, String orderUuid) {
        List<IssuedTicket> failIssuedTickets = issuedTicketAdaptor.findAllByOrderUuid(orderUuid);
        TicketItem ticketItem = ticketItemAdaptor.queryTicketItem(itemId);
        failIssuedTickets.forEach(
                issuedTicket -> {
                    ticketItem.increaseQuantity(1L);
                    issuedTicket.cancel();
                });
    }

    /*
    발급 티켓 입장 처리 로직
     */
    public IssuedTicketInfoVo processingEntranceIssuedTicket(Long eventId, Long issuedTicketId) {
        IssuedTicket issuedTicket = issuedTicketAdaptor.queryIssuedTicket(issuedTicketId);
        issuedTicketValidator.validIssuedTicketEventIdEqualEvent(issuedTicket, eventId);
        issuedTicket.entrance();
        return issuedTicket.toIssuedTicketInfoVo();
    }

    /*
    티켓 발급 로직
     */
    @RedissonLock(LockName = "티켓관리", identifier = "itemId")
    public void createIssuedTicket(Long itemId, String orderUuid, Long userId) {
        TicketItem ticketItem = ticketItemAdaptor.queryTicketItem(itemId);
        User user = userAdaptor.queryUser(userId);
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        List<OrderLineItem> orderLineItems = order.getOrderLineItems();
        List<IssuedTicket> issuedTickets =
                orderLineItems.stream()
                        .map(
                                orderLineItem ->
                                        IssuedTicket.orderLineToIssuedTicket(
                                                ticketItem,
                                                user,
                                                order,
                                                order.getEventId(),
                                                orderLineItem))
                        .flatMap(Collection::stream)
                        .toList();
        issuedTicketAdaptor.saveAll(issuedTickets);
        ticketItem.reduceQuantity((long) issuedTickets.size());
    }
}
