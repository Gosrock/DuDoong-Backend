package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class EmptyAccountNumberException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new EmptyAccountNumberException();

    private EmptyAccountNumberException() {
        super(TicketItemErrorCode.EMPTY_ACCOUT_NUMBER);
    }
}
