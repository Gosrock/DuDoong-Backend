package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class EventOpenTimeExpiredException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new EventOpenTimeExpiredException();

    private EventOpenTimeExpiredException() {
        super(EventErrorCode.OPEN_TIME_EXPIRED);
    }
}
