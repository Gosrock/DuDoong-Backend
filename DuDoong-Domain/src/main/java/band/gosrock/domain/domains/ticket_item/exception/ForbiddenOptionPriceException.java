package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class ForbiddenOptionPriceException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new ForbiddenOptionPriceException();

    private ForbiddenOptionPriceException() {
        super(TicketItemErrorCode.FORBIDDEN_OPTION_PRICE);
    }
}
