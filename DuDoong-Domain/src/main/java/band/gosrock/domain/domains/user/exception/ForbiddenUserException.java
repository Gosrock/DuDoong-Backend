package band.gosrock.domain.domains.user.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.GlobalErrorCode;

public class ForbiddenUserException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new ForbiddenUserException();

    private ForbiddenUserException() {
        super(GlobalErrorCode.USER_FORBIDDEN);
    }
}
