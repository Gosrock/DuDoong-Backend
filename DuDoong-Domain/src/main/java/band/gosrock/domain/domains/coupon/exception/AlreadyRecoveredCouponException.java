package band.gosrock.domain.domains.coupon.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class AlreadyRecoveredCouponException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new AlreadyRecoveredCouponException();

    private AlreadyRecoveredCouponException() {
        super(CouponErrorCode.ALREADY_RECOVERED_COUPON);
    }
}
