package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class LessThanMinmumPaymentOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new LessThanMinmumPaymentOrderException();

    private LessThanMinmumPaymentOrderException() {
        super(OrderErrorCode.ORDER_LESS_THAN_MINIMUM);
    }
}
