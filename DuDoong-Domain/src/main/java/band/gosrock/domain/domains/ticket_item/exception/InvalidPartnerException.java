package band.gosrock.domain.domains.ticket_item.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class InvalidPartnerException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new InvalidPartnerException();

    private InvalidPartnerException() {
        super(TicketItemErrorCode.INVALID_PARTNER);
    }
}
