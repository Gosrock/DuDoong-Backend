package band.gosrock.domain.domains.issuedTicket.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.common.vo.IssuedTicketInfoVo;
import band.gosrock.domain.domains.event.exception.HostNotAuthEventException;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssuedTicketDomainService {
    private final IssuedTicketAdaptor issuedTicketAdaptor;
    private final TicketItemAdaptor ticketItemAdaptor;

    private final OrderToIssuedTicketService orderToIssuedTicketService;

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
        TicketItem ticketItem = ticketItemAdaptor.queryTicketItem(itemId);
        List<IssuedTicket> issuedTickets =
                orderToIssuedTicketService.execute(ticketItem, orderUuid, userId);
        issuedTicketAdaptor.saveAll(issuedTickets);
        ticketItem.reduceQuantity((long) issuedTickets.size());
    }
}
