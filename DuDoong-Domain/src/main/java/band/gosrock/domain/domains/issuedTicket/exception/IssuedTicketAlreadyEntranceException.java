package band.gosrock.domain.domains.issuedTicket.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class IssuedTicketAlreadyEntranceException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new IssuedTicketAlreadyEntranceException();

    private IssuedTicketAlreadyEntranceException() {
        super(IssuedTicketErrorCode.ISSUED_TICKET_ALREADY_ENTRANCE);
    }
}
