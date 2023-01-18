package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class OptionNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new OptionNotFoundException();

    private OptionNotFoundException() {
        super(TicketItemErrorCode.OPTION_NOT_FOUND);
    }
}
