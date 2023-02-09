package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class AlreadyCloseStatusException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new AlreadyCloseStatusException();

    private AlreadyCloseStatusException() {
        super(EventErrorCode.ALREADY_CLOSE_STATUS);
    }
}
