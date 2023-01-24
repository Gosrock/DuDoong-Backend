package band.gosrock.domain.domains.coupon.exception;

import band.gosrock.common.exception.DuDoongCodeException;

public class AlreadyUsedCouponException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new AlreadyUsedCouponException();
    private AlreadyUsedCouponException() {
        super(CouponErrorCode.ALREADY_USED_COUPON);
    }
}