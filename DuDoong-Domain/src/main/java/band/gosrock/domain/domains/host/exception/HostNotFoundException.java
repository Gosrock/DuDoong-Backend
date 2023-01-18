package band.gosrock.domain.domains.host.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class HostNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new HostNotFoundException();

    private HostNotFoundException() {
        super(HostErrorCode.HOST_NOT_FOUND);
    }
}
