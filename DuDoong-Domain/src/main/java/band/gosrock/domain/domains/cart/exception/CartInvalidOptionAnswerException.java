package band.gosrock.domain.domains.cart.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CartInvalidOptionAnswerException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CartInvalidOptionAnswerException();

    private CartInvalidOptionAnswerException() {
        super(CartErrorCode.CART_INVALID_OPTION_ANSWER);
    }
}
