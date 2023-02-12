package band.gosrock.domain.domains.host.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class InvalidSlackUrlException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new InvalidSlackUrlException();

    private InvalidSlackUrlException() {
        super(HostErrorCode.INVALID_SLACK_URL);
    }
}
