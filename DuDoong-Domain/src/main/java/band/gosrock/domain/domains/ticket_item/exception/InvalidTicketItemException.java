package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class InvalidTicketItemException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new InvalidTicketItemException();

    private InvalidTicketItemException() {
        super(TicketItemErrorCode.INVALID_TICKET_ITEM);
    }
}
