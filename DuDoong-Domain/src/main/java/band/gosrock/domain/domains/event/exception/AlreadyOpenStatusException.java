package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class AlreadyOpenStatusException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new AlreadyOpenStatusException();

    private AlreadyOpenStatusException() {
        super(EventErrorCode.ALREADY_OPEN_STATUS);
    }
}
