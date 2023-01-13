package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;

public class NotPaymentOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new NotPaymentOrderException();

    private NotPaymentOrderException() {
        super(ErrorCode.ORDER_NOT_PAYMENT);
    }
}
