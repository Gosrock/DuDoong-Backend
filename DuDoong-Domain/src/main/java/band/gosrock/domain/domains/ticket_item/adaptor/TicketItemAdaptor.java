package band.gosrock.domain.domains.ticket_item.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.exception.TicketItemNotFoundException;
import band.gosrock.domain.domains.ticket_item.repository.TicketItemRepository;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class TicketItemAdaptor {

    private final TicketItemRepository ticketItemRepository;

    public TicketItem queryTicketItem(Long ticketItemId) {
        return ticketItemRepository
                .findById(ticketItemId)
                .orElseThrow(() -> TicketItemNotFoundException.EXCEPTION);
    }
}
