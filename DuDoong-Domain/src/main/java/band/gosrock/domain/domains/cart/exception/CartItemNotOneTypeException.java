package band.gosrock.domain.domains.cart.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CartItemNotOneTypeException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CartItemNotOneTypeException();

    private CartItemNotOneTypeException() {
        super(CartErrorCode.CART_INVALID_ITEM_KIND_POLICY);
    }
}
