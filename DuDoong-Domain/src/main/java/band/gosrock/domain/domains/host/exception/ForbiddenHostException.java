package band.gosrock.domain.domains.host.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class ForbiddenHostException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new ForbiddenHostException();

    private ForbiddenHostException() {
        super(HostErrorCode.FORBIDDEN_HOST);
    }
}
