package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class ApproveWaitingOrderPurchaseLimitException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION =
            new ApproveWaitingOrderPurchaseLimitException();

    private ApproveWaitingOrderPurchaseLimitException() {
        super(OrderErrorCode.APPROVE_WAITING_PURCHASE_LIMIT);
    }
}
