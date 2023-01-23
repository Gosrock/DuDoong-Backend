package band.gosrock.domain.domains.cart.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CartLineItemNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CartLineItemNotFoundException();

    private CartLineItemNotFoundException() {
        super(CartErrorCode.CART_LINE_NOT_FOUND);
    }
}
