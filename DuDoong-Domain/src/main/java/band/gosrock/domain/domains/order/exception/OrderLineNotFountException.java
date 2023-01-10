package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;

public class OrderLineNotFountException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new OrderLineNotFountException();

    private OrderLineNotFountException() {
        super(ErrorCode.ORDER_LINE_NOT_FOUND);
    }
}
