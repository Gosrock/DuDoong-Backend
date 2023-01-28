package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class InvalidOptionGroupException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new InvalidOptionGroupException();

    private InvalidOptionGroupException() {
        super(TicketItemErrorCode.INVALID_OPTION_GROUP);
    }
}
