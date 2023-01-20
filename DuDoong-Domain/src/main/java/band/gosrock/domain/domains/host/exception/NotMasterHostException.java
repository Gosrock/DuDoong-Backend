package band.gosrock.domain.domains.host.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NotMasterHostException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new NotMasterHostException();

    private NotMasterHostException() {
        super(HostErrorCode.NOT_MASTER_HOST);
    }
}
