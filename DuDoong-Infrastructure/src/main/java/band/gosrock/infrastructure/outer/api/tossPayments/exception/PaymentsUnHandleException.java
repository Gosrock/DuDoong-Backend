package band.gosrock.infrastructure.outer.api.tossPayments.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.GlobalErrorCode;

public class PaymentsUnHandleException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new PaymentsUnHandleException();

    private PaymentsUnHandleException() {
        super(GlobalErrorCode.TOSS_PAYMENTS_UNHANDLED);
    }
}
