package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class TicketItemQuantityException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new TicketItemQuantityException();

    private TicketItemQuantityException() {
        super(TicketItemErrorCode.TICKET_ITEM_QUANTITY_LESS_THAN_ZERO);
    }
}
