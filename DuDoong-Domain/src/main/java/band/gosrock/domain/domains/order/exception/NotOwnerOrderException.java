package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;

public class NotOwnerOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new NotOwnerOrderException();

    private NotOwnerOrderException() {
        super(OrderErrorCode.ORDER_NOT_MINE);
    }
}
