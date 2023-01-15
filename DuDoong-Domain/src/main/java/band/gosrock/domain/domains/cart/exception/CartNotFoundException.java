package band.gosrock.domain.domains.cart.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.GlobalErrorCode;

public class CartNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CartNotFoundException();

    private CartNotFoundException() {
        super(CartErrorCode.CART_NOT_FOUND);
    }
}
