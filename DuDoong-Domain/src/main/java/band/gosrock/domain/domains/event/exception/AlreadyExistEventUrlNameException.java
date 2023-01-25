package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class AlreadyExistEventUrlNameException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new AlreadyExistEventUrlNameException();

    private AlreadyExistEventUrlNameException() {
        super(EventErrorCode.EVENT_URL_NAME_ALREADY_EXIST);
    }
}
