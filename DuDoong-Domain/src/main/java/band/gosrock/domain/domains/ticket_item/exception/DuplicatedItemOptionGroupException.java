package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class DuplicatedItemOptionGroupException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new DuplicatedItemOptionGroupException();

    private DuplicatedItemOptionGroupException() {
        super(TicketItemErrorCode.DUPLICATED_ITEM_OPTION_GROUP);
    }
}
