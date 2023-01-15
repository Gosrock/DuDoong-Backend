package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;

public class NotApprovalOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new NotApprovalOrderException();

    private NotApprovalOrderException() {
        super(ErrorCode.ORDER_NOT_APPROVAL);
    }
}
