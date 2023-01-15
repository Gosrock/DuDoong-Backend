package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class InvalidOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new InvalidOrderException();

    private InvalidOrderException() {
        super(OrderErrorCode.ORDER_NOT_VALID);
    }
}
