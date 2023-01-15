package band.gosrock.domain.domains.user.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.GlobalErrorCode;

public class AlreadySignUpUserException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new AlreadySignUpUserException();

    private AlreadySignUpUserException() {
        super(GlobalErrorCode.USER_ALREADY_SIGNUP);
    }
}
