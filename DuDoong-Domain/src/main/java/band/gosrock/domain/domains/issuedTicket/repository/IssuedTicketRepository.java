package band.gosrock.domain.domains.issuedTicket.repository;


import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuedTicketRepository extends JpaRepository<IssuedTicket, Long> {}