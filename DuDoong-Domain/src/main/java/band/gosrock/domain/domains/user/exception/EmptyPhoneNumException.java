package band.gosrock.domain.domains.user.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class EmptyPhoneNumException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new EmptyPhoneNumException();

    private EmptyPhoneNumException() {
        super(UserErrorCode.USER_PHONE_EMPTY);
    }
}
