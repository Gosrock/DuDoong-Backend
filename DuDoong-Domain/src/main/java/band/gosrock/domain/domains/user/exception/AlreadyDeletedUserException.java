package band.gosrock.domain.domains.user.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;

public class AlreadyDeletedUserException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new AlreadyDeletedUserException();

    private AlreadyDeletedUserException() {
        super(ErrorCode.USER_ALREADY_DELETED);
    }
}
