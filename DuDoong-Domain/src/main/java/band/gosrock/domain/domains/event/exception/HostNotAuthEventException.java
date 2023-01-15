package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.GlobalErrorCode;

public class HostNotAuthEventException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new HostNotAuthEventException();

    private HostNotAuthEventException() {
        super(GlobalErrorCode.HOST_NOT_AUTH_EVENT);
    }
}
