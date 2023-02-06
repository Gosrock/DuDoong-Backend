package band.gosrock.domain.domains.coupon.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import band.gosrock.domain.domains.coupon.exception.AlreadyIssuedCouponException;
import band.gosrock.domain.domains.coupon.exception.CouponNotFoundException;
import band.gosrock.domain.domains.coupon.repository.IssuedCouponRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class IssuedCouponAdaptor {
    private final IssuedCouponRepository issuedCouponRepository;

    public List<IssuedCoupon> findAllByUserId(Long userId) {
        return issuedCouponRepository.findAllByUserId(userId);
    }

    public IssuedCoupon save(IssuedCoupon issuedCoupon) {
        return issuedCouponRepository.save(issuedCoupon);
    }

    public void exist(Long couponCampaignId, Long userId) {
        issuedCouponRepository
                .findByCouponCampaignIdAndUserId(couponCampaignId, userId)
                .ifPresent(
                        l -> {
                            throw AlreadyIssuedCouponException.EXCEPTION;
                        });
    }

    public IssuedCoupon query(Long issuedCouponId) {
        return issuedCouponRepository
                .findById(issuedCouponId)
                .orElseThrow(() -> CouponNotFoundException.EXCEPTION);
    }
}
