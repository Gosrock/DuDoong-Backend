package band.gosrock.domain.domains.issuedTicket.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.dto.condtion.IssuedTicketCondition;
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketNotFoundException;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepositoryImpl;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Adaptor
@RequiredArgsConstructor
public class IssuedTicketAdaptor {

    private final IssuedTicketRepository issuedTicketRepository;
    private final IssuedTicketRepositoryImpl issuedTicketRepositoryImpl;

    public IssuedTicket save(IssuedTicket issuedTicket) {
        return issuedTicketRepository.save(issuedTicket);
    }

    public void saveAll(List<IssuedTicket> issuedTickets) {
        issuedTicketRepository.saveAll(issuedTickets);
    }

    public List<IssuedTicket> findAllByOrderLineId(Long orderLineId) {
        return issuedTicketRepository.findAllByOrderLineId(orderLineId);
    }

    public IssuedTicket find(Long issuedTicket) {
        return issuedTicketRepository
                .findById(issuedTicket)
                .orElseThrow(() -> IssuedTicketNotFoundException.EXCEPTION);
    }

    public Page<IssuedTicket> searchIssuedTicket(Long page, IssuedTicketCondition condition) {
        PageRequest pageRequest =
                PageRequest.of(Math.toIntExact(page), 10, Sort.by("id").descending());
        return issuedTicketRepositoryImpl.search(condition, pageRequest);
    }
}
