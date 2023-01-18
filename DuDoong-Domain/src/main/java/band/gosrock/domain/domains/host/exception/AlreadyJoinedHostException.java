package band.gosrock.domain.domains.host.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class AlreadyJoinedHostException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new AlreadyJoinedHostException();

    private AlreadyJoinedHostException() {
        super(HostErrorCode.ALREADY_JOINED_HOST);
    }
}
