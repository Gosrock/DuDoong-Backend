package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class EventNotOpenException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new EventNotOpenException();

    private EventNotOpenException() {
        super(EventErrorCode.EVENT_NOT_OPEN);
    }
}
