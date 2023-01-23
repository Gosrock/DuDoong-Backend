package band.gosrock.domain.domains.host.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class HostUserNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new HostUserNotFoundException();

    private HostUserNotFoundException() {
        super(HostErrorCode.HOST_USER_NOT_FOUND);
    }
}
