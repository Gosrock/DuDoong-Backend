package band.gosrock.domain.domains.issuedTicket.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CanNotEntranceException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CanNotEntranceException();

    private CanNotEntranceException() {
        super(IssuedTicketErrorCode.CAN_NOT_ENTRANCE);
    }
}
