package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class EventTicketingTimeIsPassedException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new EventTicketingTimeIsPassedException();

    private EventTicketingTimeIsPassedException() {
        super(EventErrorCode.EVENT_TICKETING_TIME_IS_PASSED);
    }
}
