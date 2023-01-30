package band.gosrock.domain.domains.ticket_item.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.exception.TicketItemNotFoundException;
import band.gosrock.domain.domains.ticket_item.repository.TicketItemRepository;
import java.util.List;
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

    public List<TicketItem> findAllByEventId(Long eventId) {
        return ticketItemRepository.findAllByEvent_Id(eventId);
    }

    public TicketItem save(TicketItem ticketItem) {
        return ticketItemRepository.save(ticketItem);
    }
}
