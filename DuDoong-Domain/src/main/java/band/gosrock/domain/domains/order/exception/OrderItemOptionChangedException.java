package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class OrderItemOptionChangedException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new OrderItemOptionChangedException();

    private OrderItemOptionChangedException() {
        super(OrderErrorCode.ORDER_OPTION_CHANGED);
    }
}
