package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class TicketItemNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new TicketItemNotFoundException();

    private TicketItemNotFoundException() {
        super(TicketItemErrorCode.TICKET_ITEM_NOT_FOUND);
    }
}
