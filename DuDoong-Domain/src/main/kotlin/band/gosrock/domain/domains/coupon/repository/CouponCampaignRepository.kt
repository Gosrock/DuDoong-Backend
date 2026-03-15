package band.gosrock.domain.domains.coupon.repository

import band.gosrock.domain.domains.coupon.domain.CouponCampaign
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CouponCampaignRepository : JpaRepository<CouponCampaign, Long> {
    fun existsByCouponCode(couponCode: String): Boolean
    fun findByCouponCode(couponCode: String): Optional<CouponCampaign>
}
