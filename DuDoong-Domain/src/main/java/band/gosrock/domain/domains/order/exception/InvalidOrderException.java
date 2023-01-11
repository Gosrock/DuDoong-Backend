package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;

public class InvalidOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new InvalidOrderException();

    private InvalidOrderException() {
        super(ErrorCode.ORDER_NOT_VALID);
    }
}
