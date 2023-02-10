package band.gosrock.domain.domains.user.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class UserPhoneNumberInvalidException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new UserPhoneNumberInvalidException();

    private UserPhoneNumberInvalidException() {
        super(UserErrorCode.USER_PHONE_INVALID);
    }
}
