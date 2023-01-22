package band.gosrock.domain.domains.coupon.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class NoCouponStockLeftException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new NoCouponStockLeftException();

    private NoCouponStockLeftException() {
        super(CouponErrorCode.NO_COUPON_STOCK_LEFT);
    }
}
