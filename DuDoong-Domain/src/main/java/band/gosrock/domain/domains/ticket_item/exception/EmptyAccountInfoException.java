package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class EmptyAccountInfoException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new EmptyAccountInfoException();

    private EmptyAccountInfoException() {
        super(TicketItemErrorCode.EMPTY_ACCOUT_INFO);
    }
}
