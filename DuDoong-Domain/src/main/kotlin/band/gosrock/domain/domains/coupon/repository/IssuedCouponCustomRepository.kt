package band.gosrock.domain.domains.coupon.repository

import band.gosrock.domain.domains.coupon.domain.IssuedCoupon

interface IssuedCouponCustomRepository {
    fun findAllByUserId(userId: Long): List<IssuedCoupon>
}
