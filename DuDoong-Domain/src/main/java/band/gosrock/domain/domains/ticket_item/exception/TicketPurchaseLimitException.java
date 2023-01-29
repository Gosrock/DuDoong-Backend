package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class TicketPurchaseLimitException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new TicketPurchaseLimitException();

    private TicketPurchaseLimitException() {
        super(TicketItemErrorCode.TICKET_ITEM_PURCHASE_LIMIT);
    }
}
