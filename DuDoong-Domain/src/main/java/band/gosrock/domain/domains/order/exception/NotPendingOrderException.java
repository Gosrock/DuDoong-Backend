package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;

public class NotPendingOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new NotPendingOrderException();

    private NotPendingOrderException() {
        super(ErrorCode.ORDER_NOT_PENDING);
    }
}
