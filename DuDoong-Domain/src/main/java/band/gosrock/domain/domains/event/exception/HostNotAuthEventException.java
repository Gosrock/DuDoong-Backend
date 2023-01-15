package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class HostNotAuthEventException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new HostNotAuthEventException();

    private HostNotAuthEventException() {
        super(EventErrorCode.HOST_NOT_AUTH_EVENT);
    }
}
