package band.gosrock.domain.domains.user.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class UserNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new UserNotFoundException();

    private UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }
}
