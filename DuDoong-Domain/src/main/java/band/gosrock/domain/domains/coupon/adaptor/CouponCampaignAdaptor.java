package band.gosrock.domain.domains.coupon.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.coupon.domain.CouponCampaign;
import band.gosrock.domain.domains.coupon.exception.AlreadyExistCouponCampaignException;
import band.gosrock.domain.domains.coupon.exception.CouponCampaignNotFoundException;
import band.gosrock.domain.domains.coupon.repository.CouponCampaignRepository;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class CouponCampaignAdaptor {
    private final CouponCampaignRepository couponCampaignRepository;

    public CouponCampaign save(CouponCampaign couponCampaign) {
        return couponCampaignRepository.save(couponCampaign);
    }

    public void existsByCouponCode(String couponCode) {
        if (couponCampaignRepository.existsByCouponCode(couponCode))
            throw AlreadyExistCouponCampaignException.EXCEPTION;
    }

    public CouponCampaign findByCouponCode(String couponCode) {
        return couponCampaignRepository
                .findByCouponCode(couponCode)
                .orElseThrow(() -> CouponCampaignNotFoundException.EXCEPTION);
    }
}
