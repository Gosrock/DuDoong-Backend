package band.gosrock.domain.domains.coupon.repository;


import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import java.util.List;
import java.util.Optional;

public interface IssuedCouponCustomRepository {

    Optional<List<IssuedCoupon>> findAllByUserIdAndUsageStatusAndCalculateValidTerm(
            Long userId, boolean usageStatus);
}
