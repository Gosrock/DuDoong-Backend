package band.gosrock.domain.domains.cart.exception;


import band.gosrock.common.exception.DuDoongCodeException;
import band.gosrock.common.exception.ErrorCode;

public class CartNotFoundException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CartNotFoundException();

    private CartNotFoundException() {
        super(ErrorCode.CART_NOT_FOUND);
    }
}
