package band.gosrock.common.exception;

public class InvalidTokenException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new InvalidTokenException();

    private InvalidTokenException() {
        super(ErrorCode.INVALID_TOKEN);
    }
}
