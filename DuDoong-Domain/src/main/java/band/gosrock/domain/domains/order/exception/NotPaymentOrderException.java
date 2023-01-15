package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NotPaymentOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new NotPaymentOrderException();

    private NotPaymentOrderException() {
        super(OrderErrorCode.ORDER_NOT_PAYMENT);
    }
}
