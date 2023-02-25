package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CannotDeleteByOpenEventException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CannotDeleteByOpenEventException();

    private CannotDeleteByOpenEventException() {
        super(EventErrorCode.CANNOT_DELETE_BY_OPEN_EVENT);
    }
}
