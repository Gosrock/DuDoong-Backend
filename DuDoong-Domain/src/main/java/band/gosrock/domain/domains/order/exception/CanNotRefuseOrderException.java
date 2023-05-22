package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CanNotRefuseOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CanNotRefuseOrderException();

    private CanNotRefuseOrderException() {
        super(OrderErrorCode.ORDER_CANNOT_REFUSE);
    }
}
