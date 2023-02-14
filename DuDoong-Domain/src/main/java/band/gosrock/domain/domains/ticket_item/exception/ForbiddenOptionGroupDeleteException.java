package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class ForbiddenOptionGroupDeleteException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new ForbiddenOptionGroupDeleteException();

    private ForbiddenOptionGroupDeleteException() {
        super(TicketItemErrorCode.FORBIDDEN_OPTION_GROUP_DELETE);
    }
}
