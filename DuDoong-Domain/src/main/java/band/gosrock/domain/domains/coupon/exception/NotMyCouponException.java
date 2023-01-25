package band.gosrock.domain.domains.coupon.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NotMyCouponException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new NotMyCouponException();

    private NotMyCouponException() {
        super(CouponErrorCode.NOT_MY_COUPON);
    }
}
