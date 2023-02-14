package band.gosrock.domain.domains.issuedTicket.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.validator.IssuedTicketValidator;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssuedTicketDomainService {
    private final IssuedTicketAdaptor issuedTicketAdaptor;
    private final TicketItemAdaptor ticketItemAdaptor;

    private final IssuedTicketValidator issuedTicketValidator;

    private final OrderToIssuedTicketService orderToIssuedTicketService;

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

    @RedissonLock(LockName = "티켓관리", identifier = "itemId")
    public void createIssuedTicket(Long itemId, String orderUuid, Long userId) {
        TicketItem ticketItem = ticketItemAdaptor.queryTicketItem(itemId);
        List<IssuedTicket> issuedTickets =
                orderToIssuedTicketService.execute(ticketItem, orderUuid, userId);
        issuedTicketAdaptor.saveAll(issuedTickets);
        ticketItem.reduceQuantity((long) issuedTickets.size());
    }
}
