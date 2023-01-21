package band.gosrock.domain.domains.ticket_item.exception;

import band.gosrock.common.exception.DuDoongCodeException;

public class TicketItemQuantityLargeException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new TicketItemQuantityLargeException();

    private TicketItemQuantityLargeException() {
        super(TicketItemErrorCode.TICKET_ITEM_QUANTITY_LARGER_THAN_SUPPLY_COUNT);}
}
