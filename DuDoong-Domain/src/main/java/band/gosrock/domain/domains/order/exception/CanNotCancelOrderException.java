package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;

public class CanNotCancelOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CanNotCancelOrderException();

    private CanNotCancelOrderException() {
        super(ErrorCode.ORDER_CANNOT_CANCEL);
    }
}
