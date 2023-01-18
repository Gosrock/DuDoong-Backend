package band.gosrock.domain.domains.coupon.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class AlreadyExistCouponCampaignException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new AlreadyExistCouponCampaignException();

    private AlreadyExistCouponCampaignException() {
        super(CouponErrorCode.DUPLICATE_COUPON_CODE);
    }
}
