package band.gosrock.domain.domains.issuedTicket.repository;


import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuedTicketRepository
        extends JpaRepository<IssuedTicket, Long>, IssuedTicketCustomRepository {
    List<IssuedTicket> findAllByOrderLineId(Long orderLineId);

    List<IssuedTicket> findAllByOrderUuid(String orderId);

    Optional<IssuedTicket> findByIssuedTicketNo(String issuedTicketNo);
}
