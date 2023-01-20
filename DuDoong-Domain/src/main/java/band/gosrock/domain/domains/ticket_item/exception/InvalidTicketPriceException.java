package band.gosrock.domain.domains.ticket_item.exception;

import band.gosrock.common.exception.DuDoongCodeException;

public class InvalidTicketPriceException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new InvalidTicketPriceException();

    private InvalidTicketPriceException() {
        super(TicketItemErrorCode.INVALID_TICKET_PRICE);
    }
}
