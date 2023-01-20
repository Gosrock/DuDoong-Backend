package band.gosrock.domain.domains.ticket_item.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.exception.InvalidTicketPriceException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TicketItemService {

    private final TicketItemAdaptor ticketItemAdaptor;

    @Transactional
    public TicketItem createTicketItem(TicketItem ticketItem) {
        return ticketItemAdaptor.save(ticketItem);
    }

    @Transactional
    public void checkTicketPrice(Money ticketPrice) {
        if (!Money.ZERO.equals(ticketPrice)) {
            throw InvalidTicketPriceException.EXCEPTION;
        }
    }
}
