package band.gosrock.domain.domains.host.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NotSuperHostException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new NotSuperHostException();

    private NotSuperHostException() {
        super(HostErrorCode.NOT_SUPER_HOST);
    }
}
