package band.gosrock.common.exception;

public class BadLockIdentifierException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new BadLockIdentifierException();

    private BadLockIdentifierException() {
        super(ErrorCode.BAD_LOCK_IDENTIFIER);
    }
}
