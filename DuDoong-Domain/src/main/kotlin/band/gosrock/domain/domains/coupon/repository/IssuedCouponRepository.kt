package band.gosrock.domain.domains.coupon.repository

import band.gosrock.domain.domains.coupon.domain.IssuedCoupon
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface IssuedCouponRepository : JpaRepository<IssuedCoupon, Long>, IssuedCouponCustomRepository {
    fun findByCouponCampaignIdAndUserId(couponCampaignId: Long, userId: Long): Optional<IssuedCoupon>
}
