package band.gosrock.domain.domains.coupon.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.coupon.adaptor.CouponCampaignAdaptor;
import band.gosrock.domain.domains.coupon.domain.CouponCampaign;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@RequiredArgsConstructor
public class CreateCouponCampaignDomainService {

    private final CouponCampaignAdaptor couponCampaignAdaptor;

    @Transactional(readOnly = true)
    public void checkCouponCodeExists(String couponCode) {
        couponCampaignAdaptor.existsByCouponCode(couponCode);
    }

    @Transactional
    public CouponCampaign createCouponCampaign(CouponCampaign couponCampaign) {
        return couponCampaignAdaptor.save(couponCampaign);
    }
}
