package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class ForbiddenTicketItemDeleteException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new ForbiddenTicketItemDeleteException();

    private ForbiddenTicketItemDeleteException() {
        super(TicketItemErrorCode.FORBIDDEN_TICKET_ITEM_DELETE);
    }
}
