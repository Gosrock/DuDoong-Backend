package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class OrderLineNotFountException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new OrderLineNotFountException();

    private OrderLineNotFountException() {
        super(OrderErrorCode.ORDER_LINE_NOT_FOUND);
    }
}
