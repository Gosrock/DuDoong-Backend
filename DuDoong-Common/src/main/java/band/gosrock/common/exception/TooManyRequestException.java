package band.gosrock.common.exception;

public class TooManyRequestException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION =
            new TooManyRequestException();

    private TooManyRequestException() {
        super(GlobalErrorCode.TOO_MANY_REQUEST);
    }
}
