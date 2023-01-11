package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;

public class NotMyOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new NotMyOrderException();

    private NotMyOrderException() {
        super(ErrorCode.ORDER_NOT_MINE);
    }
}
