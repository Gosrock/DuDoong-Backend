package band.gosrock.domain.domains.coupon.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class SupplyLessThenMinimumException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new SupplyLessThenMinimumException();

    private SupplyLessThenMinimumException() {
        super(CouponErrorCode.SUPPLY_LESS_THEN_MINIMUM);
    }
}
