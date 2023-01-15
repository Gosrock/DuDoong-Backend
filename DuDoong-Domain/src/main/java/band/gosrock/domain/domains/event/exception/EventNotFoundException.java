package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class EventNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new EventNotFoundException();

    private EventNotFoundException() {
        super(EventErrorCode.EVENT_NOT_FOUND);
    }
}
