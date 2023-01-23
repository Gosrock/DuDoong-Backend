package band.gosrock.domain.domains.host.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class ForbiddenHostOperationException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new ForbiddenHostOperationException();

    private ForbiddenHostOperationException() {
        super(HostErrorCode.FORBIDDEN_HOST_OPERATION);
    }
}
