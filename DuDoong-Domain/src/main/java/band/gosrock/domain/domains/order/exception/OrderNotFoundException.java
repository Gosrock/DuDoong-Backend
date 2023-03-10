package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class OrderNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new OrderNotFoundException();

    private OrderNotFoundException() {
        super(OrderErrorCode.ORDER_NOT_FOUND);
    }
}
