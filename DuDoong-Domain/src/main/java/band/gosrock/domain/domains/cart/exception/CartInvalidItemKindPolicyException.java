package band.gosrock.domain.domains.cart.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CartInvalidItemKindPolicyException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CartInvalidItemKindPolicyException();

    private CartInvalidItemKindPolicyException() {
        super(CartErrorCode.CART_INVALID_ITEM_KIND_POLICY);
    }
}
