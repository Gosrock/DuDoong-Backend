package band.gosrock.domain.domains.event.exception;

import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketNotFoundException;

public class EventNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new EventNotFoundException();

    private EventNotFoundException() {
        super(ErrorCode.EVENT_NOT_FOUND);
    }
}
