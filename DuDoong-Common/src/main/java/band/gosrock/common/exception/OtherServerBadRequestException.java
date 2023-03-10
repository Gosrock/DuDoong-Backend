package band.gosrock.common.exception;

public class OtherServerBadRequestException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new OtherServerBadRequestException();

    private OtherServerBadRequestException() {
        super(GlobalErrorCode.OTHER_SERVER_BAD_REQUEST);
    }
}
