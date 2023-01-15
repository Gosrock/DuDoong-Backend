package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NotPendingOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new NotPendingOrderException();

    private NotPendingOrderException() {
        super(OrderErrorCode.ORDER_NOT_PENDING);
    }
}
