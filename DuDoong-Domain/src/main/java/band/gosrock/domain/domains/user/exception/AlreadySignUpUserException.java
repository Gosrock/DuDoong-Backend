package band.gosrock.domain.domains.user.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;

public class AlreadySignUpUserException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new AlreadySignUpUserException();

    private AlreadySignUpUserException() {
        super(ErrorCode.USER_ALREADY_SIGNUP);
    }
}
