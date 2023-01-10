package band.gosrock.api.issuedTicket.service;


import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateIssuedTicketUseCase {

    private final IssuedTicketDomainService issuedTicketDomainService;
    private final IssuedTicketRepository issuedTicketRepository;
}
