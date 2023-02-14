package band.gosrock.domain.domains.issuedTicket.validator;


import band.gosrock.common.annotation.Validator;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketNotMatchedEventException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@Validator
@RequiredArgsConstructor
public class IssuedTicketValidator {

    public void validIssuedTicketEventIdEqualEvent(IssuedTicket issuedTicket, Long eventId) {
        if (!Objects.equals(issuedTicket.getEventId(), eventId)) {
            throw IssuedTicketNotMatchedEventException.EXCEPTION;
        }
    }
}
