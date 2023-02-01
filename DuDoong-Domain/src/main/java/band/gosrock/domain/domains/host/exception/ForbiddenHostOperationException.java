package band.gosrock.domain.domains.host.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class ForbiddenHostOperationException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new ForbiddenHostOperationException();

    private ForbiddenHostOperationException() {
        super(HostErrorCode.CANNOT_MODIFY_MASTER_HOST_ROLE);
    }
}
