package band.gosrock.common.exception;

public class BadLockIdentifierException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new BadLockIdentifierException();

    private BadLockIdentifierException() {
        super(GlobalErrorCode.BAD_LOCK_IDENTIFIER);
    }
}
