package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;

public class CanNotRefundOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CanNotRefundOrderException();

    private CanNotRefundOrderException() {
        super(ErrorCode.ORDER_CANNOT_REFUND);
    }
}
