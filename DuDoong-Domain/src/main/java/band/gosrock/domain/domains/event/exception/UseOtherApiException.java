package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class UseOtherApiException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new UseOtherApiException();

    private UseOtherApiException() {
        super(EventErrorCode.USE_OTHER_API);
    }
}
