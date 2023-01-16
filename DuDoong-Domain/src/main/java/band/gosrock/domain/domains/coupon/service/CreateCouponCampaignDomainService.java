package band.gosrock.domain.domains.coupon.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.coupon.exception.AlreadyExistCouponCampaignException;
import band.gosrock.domain.domains.coupon.repository.CouponCampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@RequiredArgsConstructor
public class CreateCouponCampaignDomainService {

    private final CouponCampaignRepository couponCampaignRepository;

    @Transactional(readOnly = true)
    public void checkCouponCodeExists(String couponCode) {
        if (couponCampaignRepository.existsByCouponCode(couponCode)) {
            throw AlreadyExistCouponCampaignException.EXCEPTION;
        }
    }
}
