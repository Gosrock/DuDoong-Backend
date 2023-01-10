package band.gosrock.domain.domains.issuedTicket.exception;

import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;

public class IssuedTicketUserNotMatchedException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new IssuedTicketUserNotMatchedException();

    private IssuedTicketUserNotMatchedException() {
        super(ErrorCode.ISSUED_TICKET_NOT_MATCHED_USER);
    }
}
