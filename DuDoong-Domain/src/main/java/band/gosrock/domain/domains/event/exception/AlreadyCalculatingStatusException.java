package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class AlreadyCalculatingStatusException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new AlreadyCalculatingStatusException();

    private AlreadyCalculatingStatusException() {
        super(EventErrorCode.ALREADY_CALCULATING_STATUS);
    }
}
