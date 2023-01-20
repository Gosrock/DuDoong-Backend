package band.gosrock.domain.domains.ticket_item.exception;

import band.gosrock.common.exception.DuDoongCodeException;

public class TicketItemQuantityLackException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new TicketItemQuantityLackException();

    private TicketItemQuantityLackException() {super(TicketItemErrorCode.TICKET_ITEM_QUANTITY_LACK);}

}
