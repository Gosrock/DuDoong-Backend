package band.gosrock.domain.domains.coupon.repository;


import band.gosrock.domain.domains.coupon.domain.CouponCampaign;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuedCouponRepository
        extends JpaRepository<IssuedCoupon, Long>, IssuedCouponCustomRepository {
    Optional<IssuedCoupon> findByCouponCampaignAndUserId(
            CouponCampaign couponCampaign, Long userId);
}
