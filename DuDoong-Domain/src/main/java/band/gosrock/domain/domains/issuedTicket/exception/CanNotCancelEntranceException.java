package band.gosrock.domain.domains.issuedTicket.exception;

import band.gosrock.common.exception.DuDoongCodeException;

public class CanNotCancelEntranceException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CanNotCancelEntranceException();

    private CanNotCancelEntranceException() {
        super(IssuedTicketErrorCode.CAN_NOT_CANCEL_ENTRANCE);
    }
}
