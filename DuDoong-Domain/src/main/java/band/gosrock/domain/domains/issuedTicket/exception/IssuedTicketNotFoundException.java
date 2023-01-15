package band.gosrock.domain.domains.issuedTicket.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class IssuedTicketNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new IssuedTicketNotFoundException();

    private IssuedTicketNotFoundException() {
        super(IssuedTicketErrorCode.ISSUED_TICKET_NOT_FOUND);
    }
}
