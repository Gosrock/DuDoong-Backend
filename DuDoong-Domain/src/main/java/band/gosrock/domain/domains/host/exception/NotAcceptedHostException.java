package band.gosrock.domain.domains.host.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NotAcceptedHostException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new NotAcceptedHostException();

    private NotAcceptedHostException() {
        super(HostErrorCode.NOT_ACCEPTED_HOST);
    }
}
