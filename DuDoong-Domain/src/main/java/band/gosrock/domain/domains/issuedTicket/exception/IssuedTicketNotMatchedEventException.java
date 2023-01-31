package band.gosrock.domain.domains.issuedTicket.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class IssuedTicketNotMatchedEventException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new IssuedTicketNotMatchedEventException();

    private IssuedTicketNotMatchedEventException() {
        super(IssuedTicketErrorCode.ISSUED_TICKET_NOT_MATCHED_EVENT);
    }
}
