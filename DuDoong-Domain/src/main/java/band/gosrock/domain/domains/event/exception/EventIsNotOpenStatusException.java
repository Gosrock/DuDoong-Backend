package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class EventIsNotOpenStatusException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new EventIsNotOpenStatusException();

    private EventIsNotOpenStatusException() {
        super(EventErrorCode.EVENT_NOT_OPEN);
    }
}
