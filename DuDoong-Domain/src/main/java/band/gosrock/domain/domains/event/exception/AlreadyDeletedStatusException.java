package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class AlreadyDeletedStatusException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new AlreadyDeletedStatusException();

    private AlreadyDeletedStatusException() {
        super(EventErrorCode.ALREADY_DELETED_STATUS);
    }
}
