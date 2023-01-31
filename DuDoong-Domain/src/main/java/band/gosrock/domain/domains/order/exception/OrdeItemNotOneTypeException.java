package band.gosrock.domain.domains.order.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class OrdeItemNotOneTypeException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new OrdeItemNotOneTypeException();

    private OrdeItemNotOneTypeException() {
        super(OrderErrorCode.ORDER_INVALID_ITEM_KIND_POLICY);
    }
}
