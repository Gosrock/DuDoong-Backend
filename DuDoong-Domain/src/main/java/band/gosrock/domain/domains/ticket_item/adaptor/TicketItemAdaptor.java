package band.gosrock.domain.domains.ticket_item.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.domain.TicketItemStatus;
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
                .findByIdAndTicketItemStatus(ticketItemId, TicketItemStatus.VALID)
                .orElseThrow(() -> TicketItemNotFoundException.EXCEPTION);
    }

    public List<TicketItem> findAllByEventId(Long eventId) {
        return ticketItemRepository.findAllByEvent_IdAndTicketItemStatus(
                eventId, TicketItemStatus.VALID);
    }

    public TicketItem save(TicketItem ticketItem) {
        return ticketItemRepository.save(ticketItem);
    }
}
