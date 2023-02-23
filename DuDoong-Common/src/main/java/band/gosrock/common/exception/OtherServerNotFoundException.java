package band.gosrock.common.exception;

public class OtherServerNotFoundException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new OtherServerNotFoundException();

    private OtherServerNotFoundException() {
        super(GlobalErrorCode.OTHER_SERVER_NOT_FOUND);
    }
}
