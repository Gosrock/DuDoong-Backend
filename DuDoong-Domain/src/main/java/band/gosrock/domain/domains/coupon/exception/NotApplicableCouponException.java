package band.gosrock.domain.domains.coupon.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NotApplicableCouponException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new NotApplicableCouponException();

    private NotApplicableCouponException() {
        super(CouponErrorCode.NOT_APPLICABLE_COUPON);
    }
}
