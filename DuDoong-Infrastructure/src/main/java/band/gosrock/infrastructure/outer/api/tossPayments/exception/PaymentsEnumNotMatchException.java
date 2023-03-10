package band.gosrock.infrastructure.outer.api.tossPayments.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.GlobalErrorCode;

public class PaymentsEnumNotMatchException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new PaymentsEnumNotMatchException();

    private PaymentsEnumNotMatchException() {
        super(GlobalErrorCode.TOSS_PAYMENTS_ENUM_NOT_MATCH);
    }
}
