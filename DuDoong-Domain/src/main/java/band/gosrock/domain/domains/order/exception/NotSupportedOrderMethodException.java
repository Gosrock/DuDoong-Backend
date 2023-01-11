package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;

public class NotSupportedOrderMethodException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new NotSupportedOrderMethodException();

    private NotSupportedOrderMethodException() {
        super(ErrorCode.ORDER_NOT_SUPPORTED_METHOD);
    }
}
