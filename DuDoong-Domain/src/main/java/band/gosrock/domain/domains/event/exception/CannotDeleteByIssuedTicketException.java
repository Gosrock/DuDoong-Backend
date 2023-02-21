package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CannotDeleteByIssuedTicketException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CannotDeleteByIssuedTicketException();

    private CannotDeleteByIssuedTicketException() {
        super(EventErrorCode.CANNOT_DELETE_BY_ISSUED_TICKET);
    }
}
