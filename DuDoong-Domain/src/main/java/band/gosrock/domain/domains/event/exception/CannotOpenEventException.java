package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CannotOpenEventException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CannotOpenEventException();

    private CannotOpenEventException() {
        super(EventErrorCode.CANNOT_OPEN_EVENT);
    }
}
