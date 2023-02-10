package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class InvalidTicketTypeException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new InvalidTicketTypeException();

    private InvalidTicketTypeException() {
        super(TicketItemErrorCode.INVALID_TICKET_TYPE);
    }
}
