package band.gosrock.domain.domains.host.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NotManagerHostException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new NotManagerHostException();

    private NotManagerHostException() {
        super(HostErrorCode.NOT_MANAGER_HOST);
    }
}
