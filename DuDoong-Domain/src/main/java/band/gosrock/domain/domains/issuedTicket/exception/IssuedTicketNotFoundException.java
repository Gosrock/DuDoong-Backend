package band.gosrock.domain.domains.issuedTicket.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.GlobalErrorCode;

public class IssuedTicketNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new IssuedTicketNotFoundException();

    private IssuedTicketNotFoundException() {
        super(GlobalErrorCode.ISSUED_TICKET_NOT_FOUND);
    }
}
