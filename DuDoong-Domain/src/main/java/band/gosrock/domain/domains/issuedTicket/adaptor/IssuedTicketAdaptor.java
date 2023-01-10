package band.gosrock.domain.domains.issuedTicket.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketNotFoundException;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class IssuedTicketAdaptor {

    private final IssuedTicketRepository issuedTicketRepository;

    public IssuedTicket save(IssuedTicket issuedTicket) {
        return issuedTicketRepository.save(issuedTicket);
    }

    public IssuedTicket find(Long issuedTicket) {
        return issuedTicketRepository
                .findById(issuedTicket)
                .orElseThrow(() -> IssuedTicketNotFoundException.EXCEPTION);
    }
}
