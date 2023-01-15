package band.gosrock.domain.domains.user.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class ForbiddenUserException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new ForbiddenUserException();

    private ForbiddenUserException() {
        super(UserErrorCode.USER_FORBIDDEN);
    }
}
