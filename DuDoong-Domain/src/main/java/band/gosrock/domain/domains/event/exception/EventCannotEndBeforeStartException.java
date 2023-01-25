package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class EventCannotEndBeforeStartException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new EventCannotEndBeforeStartException();

    private EventCannotEndBeforeStartException() {
        super(EventErrorCode.EVENT_CANNOT_END_BEFORE_START);
    }
}
