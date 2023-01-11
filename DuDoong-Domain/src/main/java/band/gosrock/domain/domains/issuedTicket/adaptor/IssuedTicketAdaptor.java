package band.gosrock.domain.domains.issuedTicket.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketNotFoundException;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Adaptor
@RequiredArgsConstructor
public class IssuedTicketAdaptor {

    private final IssuedTicketRepository issuedTicketRepository;

    public IssuedTicket save(IssuedTicket issuedTicket) {
        return issuedTicketRepository.save(issuedTicket);
    }

    public List<IssuedTicket> findAllByOrderLineId(Long orderLineId) {
        return issuedTicketRepository.findAllByOrderLineId(orderLineId);
    }

    public IssuedTicket find(Long issuedTicket) {
        return issuedTicketRepository
                .findById(issuedTicket)
                .orElseThrow(() -> IssuedTicketNotFoundException.EXCEPTION);
    }

    public Page<IssuedTicket> findAllByEvent(PageRequest pageRequest, Long eventId) {
        return issuedTicketRepository.findAllByEvent_IdOrderByIdDesc(eventId, pageRequest);
    }

    public Page<IssuedTicket> findAllByEventAndUserName(
            PageRequest pageRequest, Long eventId, String userName) {
        return issuedTicketRepository.findAllByEvent_IdAndUser_Profile_NameLike(
                eventId, userName, pageRequest);
    }
}
