package band.gosrock.domain.domains.user.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.GlobalErrorCode;

public class AlreadyDeletedUserException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new AlreadyDeletedUserException();

    private AlreadyDeletedUserException() {
        super(GlobalErrorCode.USER_ALREADY_DELETED);
    }
}
