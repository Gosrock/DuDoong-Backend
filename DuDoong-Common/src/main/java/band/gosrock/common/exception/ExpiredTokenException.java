package band.gosrock.common.exception;

public class ExpiredTokenException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new ExpiredTokenException();

    private ExpiredTokenException() {
        super(GlobalErrorCode.TOKEN_EXPIRED);
    }
}
