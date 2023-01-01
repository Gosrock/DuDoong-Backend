package band.gosrock.common.exception;

public class OtherServerUnauthorizedException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new OtherServerUnauthorizedException();

    private OtherServerUnauthorizedException() {
        super(ErrorCode.OTHER_SERVER_UNAUTHORIZED);
    }
}
