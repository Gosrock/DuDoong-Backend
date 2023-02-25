package band.gosrock.common.exception;

public class OtherServerInternalSeverErrorException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION =
            new OtherServerInternalSeverErrorException();

    private OtherServerInternalSeverErrorException() {
        super(GlobalErrorCode.OTHER_SERVER_INTERNAL_SERVER_ERROR);
    }
}
