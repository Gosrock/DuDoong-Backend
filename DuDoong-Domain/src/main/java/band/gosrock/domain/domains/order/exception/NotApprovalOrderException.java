package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NotApprovalOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new NotApprovalOrderException();

    private NotApprovalOrderException() {
        super(OrderErrorCode.ORDER_NOT_APPROVAL);
    }
}
