package band.gosrock.common.exception;

public class OtherServerExpiredTokenException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new OtherServerExpiredTokenException();

    private OtherServerExpiredTokenException() {
        super(GlobalErrorCode.OTHER_SERVER_EXPIRED_TOKEN);
    }
}
