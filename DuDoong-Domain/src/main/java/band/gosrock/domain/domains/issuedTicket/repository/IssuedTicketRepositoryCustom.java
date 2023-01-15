package band.gosrock.domain.domains.issuedTicket.repository;


import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.dto.condtion.IssuedTicketCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IssuedTicketRepositoryCustom {

    Page<IssuedTicket> search(IssuedTicketCondition condition, Pageable pageable);
}
