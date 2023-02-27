package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CanNotApproveDeletedUserOrderException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CanNotApproveDeletedUserOrderException();

    private CanNotApproveDeletedUserOrderException() {
        super(OrderErrorCode.CAN_NOT_DELETED_USER_APPROVE);
    }
}
