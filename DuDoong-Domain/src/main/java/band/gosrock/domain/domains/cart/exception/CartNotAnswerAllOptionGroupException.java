package band.gosrock.domain.domains.cart.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CartNotAnswerAllOptionGroupException extends DuDoongCodeException {

    public static final DuDoongCodeException EXCEPTION = new CartNotAnswerAllOptionGroupException();

    private CartNotAnswerAllOptionGroupException() {
        super(CartErrorCode.CART_NOT_ALL_ANSWER);
    }
}
