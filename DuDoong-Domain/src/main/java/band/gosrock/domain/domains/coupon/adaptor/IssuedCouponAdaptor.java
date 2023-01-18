package band.gosrock.domain.domains.coupon.adaptor;


import band.gosrock.common.annotation.Adaptor;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import band.gosrock.domain.domains.coupon.repository.IssuedCouponRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@Adaptor
@RequiredArgsConstructor
public class IssuedCouponAdaptor {
    private final IssuedCouponRepository issuedCouponRepository;

    public Optional<List<IssuedCoupon>> findAllByUserIdAndUsageStatusAndValidTerm(Long userId) {
        Optional<List<IssuedCoupon>> issuedCoupons =
                issuedCouponRepository.findAllByUserIdAndUsageStatusAndCalculateValidTerm(
                        userId, false);
        return issuedCoupons;
    }
}
