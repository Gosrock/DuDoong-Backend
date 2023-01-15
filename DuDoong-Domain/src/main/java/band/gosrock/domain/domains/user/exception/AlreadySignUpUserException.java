package band.gosrock.domain.domains.user.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class AlreadySignUpUserException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new AlreadySignUpUserException();

    private AlreadySignUpUserException() {
        super(UserErrorCode.USER_ALREADY_SIGNUP);
    }
}
