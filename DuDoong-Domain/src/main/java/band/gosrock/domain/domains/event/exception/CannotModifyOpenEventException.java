package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CannotModifyOpenEventException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CannotModifyOpenEventException();

    private CannotModifyOpenEventException() {
        super(EventErrorCode.CANNOT_MODIFY_OPEN_EVENT);
    }
}
