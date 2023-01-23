package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NotFreeOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new NotFreeOrderException();

    private NotFreeOrderException() {
        super(OrderErrorCode.ORDER_NOT_FREE);
    }
}
