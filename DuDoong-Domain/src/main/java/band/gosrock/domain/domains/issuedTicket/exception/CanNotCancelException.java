package band.gosrock.domain.domains.issuedTicket.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CanNotCancelException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CanNotCancelException();

    private CanNotCancelException() {
        super(IssuedTicketErrorCode.CAN_NOT_CANCEL);
    }
}
