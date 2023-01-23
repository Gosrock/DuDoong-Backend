package band.gosrock.domain.domains.coupon.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NotIssuingCouponPeriodException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new NotIssuingCouponPeriodException();

    private NotIssuingCouponPeriodException() {
        super(CouponErrorCode.NOT_COUPON_ISSUING_PERIOD);
    }
}
