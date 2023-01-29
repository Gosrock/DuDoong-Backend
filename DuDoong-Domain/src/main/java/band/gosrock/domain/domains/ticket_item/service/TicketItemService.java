package band.gosrock.domain.domains.ticket_item.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TicketItemService {

    private final TicketItemAdaptor ticketItemAdaptor;

    @Transactional
    public TicketItem createTicketItem(TicketItem ticketItem, Boolean isPartner) {
        if (!isPartner) {
            ticketItem.checkTicketPrice();
        }
        return ticketItemAdaptor.save(ticketItem);
    }
}
