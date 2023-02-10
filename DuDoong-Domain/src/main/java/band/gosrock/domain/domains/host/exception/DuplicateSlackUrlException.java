package band.gosrock.domain.domains.host.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class DuplicateSlackUrlException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new DuplicateSlackUrlException();

    private DuplicateSlackUrlException() {
        super(HostErrorCode.DUPLICATED_SLACK_URL);
    }
}
