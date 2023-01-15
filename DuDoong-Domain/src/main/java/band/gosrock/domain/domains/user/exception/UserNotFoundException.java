package band.gosrock.domain.domains.user.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.GlobalErrorCode;

public class UserNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new UserNotFoundException();

    private UserNotFoundException() {
        super(GlobalErrorCode.USER_NOT_FOUND);
    }
}
