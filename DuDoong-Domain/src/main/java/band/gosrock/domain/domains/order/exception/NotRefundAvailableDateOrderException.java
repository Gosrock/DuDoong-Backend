package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NotRefundAvailableDateOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new NotRefundAvailableDateOrderException();

    private NotRefundAvailableDateOrderException() {
        super(OrderErrorCode.ORDER_NOT_REFUND_DATE);
    }
}
