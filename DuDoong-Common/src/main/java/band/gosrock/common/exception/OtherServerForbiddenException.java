package band.gosrock.common.exception;

public class OtherServerForbiddenException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new OtherServerForbiddenException();

    private OtherServerForbiddenException() {
        super(GlobalErrorCode.OTHER_SERVER_FORBIDDEN);
    }
}
