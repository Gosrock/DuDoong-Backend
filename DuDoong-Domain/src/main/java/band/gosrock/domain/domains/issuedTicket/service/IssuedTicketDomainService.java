package band.gosrock.domain.domains.issuedTicket.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class IssuedTicketDomainService {

    private final IssuedTicketRepository issuedTicketRepository;
}
