package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;

public class NotRefundAvailableDateOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new NotRefundAvailableDateOrderException();

    private NotRefundAvailableDateOrderException() {
        super(ErrorCode.ORDER_NOT_REFUND_DATE);
    }
}
