package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class ForbiddenOptionChangeException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new ForbiddenOptionChangeException();

    private ForbiddenOptionChangeException() {
        super(TicketItemErrorCode.FORBIDDEN_OPTION_CHANGE);
    }
}
