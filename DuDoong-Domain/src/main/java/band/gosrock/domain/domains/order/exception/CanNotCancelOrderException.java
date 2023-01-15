package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CanNotCancelOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CanNotCancelOrderException();

    private CanNotCancelOrderException() {
        super(OrderErrorCode.ORDER_CANNOT_CANCEL);
    }
}
