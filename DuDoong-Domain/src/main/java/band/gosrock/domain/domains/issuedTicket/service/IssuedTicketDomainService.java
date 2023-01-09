package band.gosrock.domain.domains.issuedTicket.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus;
import band.gosrock.domain.domains.issuedTicket.dto.IssuedTicketDTO;
import band.gosrock.domain.domains.issuedTicket.dto.PostIssuedTicketDTO;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssuedTicketDomainService {

    private final IssuedTicketRepository issuedTicketRepository;
}
