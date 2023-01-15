package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NotSupportedOrderMethodException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new NotSupportedOrderMethodException();

    private NotSupportedOrderMethodException() {
        super(OrderErrorCode.ORDER_NOT_SUPPORTED_METHOD);
    }
}
