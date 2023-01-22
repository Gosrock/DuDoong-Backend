package band.gosrock.domain.domains.coupon.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class AlreadyIssuedCouponException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new AlreadyIssuedCouponException();

    private AlreadyIssuedCouponException() {
        super(CouponErrorCode.ALREADY_ISSUED_COUPON);
    }
}
