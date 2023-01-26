package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CannotModifyEventBasicException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CannotModifyEventBasicException();

    private CannotModifyEventBasicException() {
        super(EventErrorCode.CANNOT_MODIFY_EVENT_BASIC);
    }
}
