package band.gosrock.domain.domains.coupon.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CouponNotFoundException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new CouponNotFoundException();

    private CouponNotFoundException() {
        super(CouponErrorCode.NOT_FOUND_COUPON);
    }
}
