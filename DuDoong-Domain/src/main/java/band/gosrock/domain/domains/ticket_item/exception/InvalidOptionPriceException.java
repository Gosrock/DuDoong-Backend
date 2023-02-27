package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class InvalidOptionPriceException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new InvalidOptionPriceException();

    private InvalidOptionPriceException() {
        super(TicketItemErrorCode.INVALID_OPTION_PRICE);
    }
}
