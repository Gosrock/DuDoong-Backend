package band.gosrock.domain.domains.ticket_item.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.exception.InvalidTicketItemException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TicketItemService {

    private final TicketItemAdaptor ticketItemAdaptor;

    @Transactional
    public TicketItem createTicketItem(TicketItem ticketItem, Boolean isPartner) {

        ticketItem.validateTicketPayType(isPartner);
        return ticketItemAdaptor.save(ticketItem);
    }

    public void validateExistenceByEventId(Long eventId) {
        if (!ticketItemAdaptor.existsByEventId(eventId)) {
            throw InvalidTicketItemException.EXCEPTION;
        }
    }

    @RedissonLock(LockName = "티켓관리", identifier = "ticketItemId")
    public void softDeleteTicketItem(Long eventId, Long ticketItemId) {

        TicketItem ticketItem = ticketItemAdaptor.queryTicketItem(ticketItemId);
        // 해당 eventId에 속해 있는 티켓 아이템이 맞는지 확인
        ticketItem.validateEventId(eventId);

        ticketItem.softDeleteTicketItem();
        ticketItemAdaptor.save(ticketItem);
    }
}
