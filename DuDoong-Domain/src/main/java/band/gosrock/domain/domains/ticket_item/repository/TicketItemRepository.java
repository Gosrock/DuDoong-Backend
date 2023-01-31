package band.gosrock.domain.domains.ticket_item.repository;


import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.domain.TicketItemStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketItemRepository extends JpaRepository<TicketItem, Long> {

    List<TicketItem> findAllByEvent_IdAndTicketItemStatus(Long eventId, TicketItemStatus status);

    Boolean existsByEvent_Id(Long eventId);

    Optional<TicketItem> findByIdAndTicketItemStatus(Long ticketItemId, TicketItemStatus status);
}
