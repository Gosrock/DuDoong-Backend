package band.gosrock.domain.domains.issuedTicket.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.GlobalErrorCode;

public class IssuedTicketUserNotMatchedException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new IssuedTicketUserNotMatchedException();

    private IssuedTicketUserNotMatchedException() {
        super(GlobalErrorCode.ISSUED_TICKET_NOT_MATCHED_USER);
    }
}
