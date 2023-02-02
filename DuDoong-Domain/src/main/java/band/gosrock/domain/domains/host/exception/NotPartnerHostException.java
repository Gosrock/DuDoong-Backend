package band.gosrock.domain.domains.host.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NotPartnerHostException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new NotPartnerHostException();

    private NotPartnerHostException() {
        super(HostErrorCode.NOT_PARTNER_HOST);
    }
}
