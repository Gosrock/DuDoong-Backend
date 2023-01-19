package band.gosrock.domain.domains.coupon.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class WrongDiscountAmountException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new WrongDiscountAmountException();

    private WrongDiscountAmountException() {
        super(CouponErrorCode.WRONG_DISCOUNT_AMOUNT);
    }
}
