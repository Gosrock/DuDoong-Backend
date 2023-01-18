package band.gosrock.domain.domains.coupon.repository;


import band.gosrock.domain.domains.coupon.domain.CouponCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponCampaignRepository extends JpaRepository<CouponCampaign, Long> {
    boolean existsByCouponCode(String couponCode);
}
