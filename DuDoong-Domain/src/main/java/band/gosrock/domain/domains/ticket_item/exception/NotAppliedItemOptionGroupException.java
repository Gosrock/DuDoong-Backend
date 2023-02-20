package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NotAppliedItemOptionGroupException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new NotAppliedItemOptionGroupException();

    private NotAppliedItemOptionGroupException() {
        super(TicketItemErrorCode.NOT_APPLIED_ITEM_OPTION_GROUP);
    }
}
