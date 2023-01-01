package band.gosrock.common.exception;

public class RefreshTokenExpiredException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new RefreshTokenExpiredException();

    private RefreshTokenExpiredException() {
        super(ErrorCode.REFRESH_TOKEN_EXPIRED);
    }
}
