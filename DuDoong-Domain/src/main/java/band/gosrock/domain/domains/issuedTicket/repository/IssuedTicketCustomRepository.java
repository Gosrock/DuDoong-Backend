package band.gosrock.domain.domains.issuedTicket.repository;


import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.dto.condtion.IssuedTicketCondition;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IssuedTicketCustomRepository {

    Page<IssuedTicket> searchToPage(IssuedTicketCondition condition, Pageable pageable);

    Optional<IssuedTicket> find(Long issuedTicketId);
}
