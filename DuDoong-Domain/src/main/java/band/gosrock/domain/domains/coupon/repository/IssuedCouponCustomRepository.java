package band.gosrock.domain.domains.coupon.repository;


import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import java.util.List;

public interface IssuedCouponCustomRepository {

    List<IssuedCoupon> findAllByUserIdAndUsageStatus(Long userId, boolean usageStatus);
}
