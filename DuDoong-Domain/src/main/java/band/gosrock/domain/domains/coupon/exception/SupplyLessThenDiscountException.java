package band.gosrock.domain.domains.coupon.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class SupplyLessThenDiscountException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new SupplyLessThenDiscountException();

    private SupplyLessThenDiscountException() {
        super(CouponErrorCode.SUPPLY_LESS_THEN_DISCOUNT);
    }
}
