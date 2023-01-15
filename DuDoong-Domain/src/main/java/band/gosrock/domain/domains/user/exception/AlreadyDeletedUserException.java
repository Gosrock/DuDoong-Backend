package band.gosrock.domain.domains.user.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class AlreadyDeletedUserException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new AlreadyDeletedUserException();

    private AlreadyDeletedUserException() {
        super(UserErrorCode.USER_ALREADY_DELETED);
    }
}
