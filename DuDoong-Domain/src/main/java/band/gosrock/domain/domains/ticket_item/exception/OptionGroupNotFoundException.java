package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class OptionGroupNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new OptionGroupNotFoundException();

    private OptionGroupNotFoundException() {
        super(TicketItemErrorCode.OPTION_GROUP_NOT_FOUND);
    }
}
