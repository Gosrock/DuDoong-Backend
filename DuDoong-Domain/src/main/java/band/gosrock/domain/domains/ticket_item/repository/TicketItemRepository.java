package band.gosrock.domain.domains.ticket_item.repository;


import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketItemRepository extends JpaRepository<TicketItem, Long> {

    List<TicketItem> findAllByEvent_Id(Long eventId);
}
