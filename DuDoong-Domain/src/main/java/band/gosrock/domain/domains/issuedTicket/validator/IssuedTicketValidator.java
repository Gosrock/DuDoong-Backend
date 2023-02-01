package band.gosrock.domain.domains.issuedTicket.validator;


import band.gosrock.common.annotation.Validator;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketNotMatchedEventException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@Validator
@RequiredArgsConstructor
public class IssuedTicketValidator {

    private final EventAdaptor eventAdaptor;
    private final HostAdaptor hostAdaptor;

    public void validCanModifyIssuedTicketUser(IssuedTicket issuedTicket, Long currentUserId) {
        Event event = eventAdaptor.findById(issuedTicket.getEventId());
        Host host = hostAdaptor.findById(event.getHostId());
        host.validateHostUser(currentUserId);
    }

    public void validIssuedTicketEventIdEqualEvent(IssuedTicket issuedTicket, Long eventId) {
        if (!Objects.equals(issuedTicket.getEventId(), eventId)) {
            throw IssuedTicketNotMatchedEventException.EXCEPTION;
        }
    }
}
