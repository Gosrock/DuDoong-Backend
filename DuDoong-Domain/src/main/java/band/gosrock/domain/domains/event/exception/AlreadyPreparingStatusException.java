package band.gosrock.domain.domains.event.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class AlreadyPreparingStatusException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new AlreadyPreparingStatusException();

    private AlreadyPreparingStatusException() {
        super(EventErrorCode.ALREADY_PREPARING_STATUS);
    }
}
