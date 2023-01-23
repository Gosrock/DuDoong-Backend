package band.gosrock.domain.domains.coupon.exception;


import band.gosrock.common.exception.DuDoongCodeException;

public class CouponCampaignNotFoundException extends DuDoongCodeException {
    public static final DuDoongCodeException EXCEPTION = new CouponCampaignNotFoundException();

    private CouponCampaignNotFoundException() {
        super(CouponErrorCode.NOT_FOUND_COUPON_CAMPAIGN);
    }
}
