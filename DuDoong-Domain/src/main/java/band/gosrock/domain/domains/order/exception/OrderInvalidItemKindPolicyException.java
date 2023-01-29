package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.domain.domains.cart.exception.CartErrorCode;

public class OrderInvalidItemKindPolicyException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new OrderInvalidItemKindPolicyException();

    private OrderInvalidItemKindPolicyException() {
        super(OrderErrorCode.ORDER_INVALID_ITEM_KIND_POLICY);
    }
}
