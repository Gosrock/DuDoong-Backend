package band.gosrock.domain.domains.coupon.repository;


import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuedCouponRepository
        extends JpaRepository<IssuedCoupon, Long>, IssuedCouponCustomRepository {}
