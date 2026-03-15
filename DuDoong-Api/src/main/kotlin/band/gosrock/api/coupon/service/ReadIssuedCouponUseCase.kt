package band.gosrock.api.coupon.service

import band.gosrock.api.common.UserUtils
import band.gosrock.api.coupon.dto.response.ReadIssuedCouponResponse
import band.gosrock.api.coupon.mapper.IssuedCouponMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon
import org.springframework.transaction.annotation.Transactional

@UseCase
class ReadIssuedCouponUseCase(
    private val userUtils: UserUtils,
    private val issuedCouponAdaptor: IssuedCouponAdaptor,
    private val issuedCouponMapper: IssuedCouponMapper,
) {
    @Transactional(readOnly = true)
    fun execute(expired: Boolean): ReadIssuedCouponResponse {
        // 존재하는 유저인지 검증
        val user = userUtils.getCurrentUser()

        val issuedCoupons = issuedCouponAdaptor.findAllByUserId(user.id!!)
        // 사용 가능 쿠폰 조회 (사용 이전, 유효 기간 이전)
        val validTermAvailableIssuedCoupons = filterAvailableCouponList(issuedCoupons)

        // 만료된 쿠폰 조회(사용 완료, 유효 기간 만료)
        if (expired) {
            val expiredValidTermIssuedCoupons = filterExpiredCouponList(issuedCoupons)
            return issuedCouponMapper.toReadIssuedCouponMyPageResponse(
                validTermAvailableIssuedCoupons,
                expiredValidTermIssuedCoupons,
            )
        }

        return issuedCouponMapper.toReadIssuedCouponMyPageResponse(
            validTermAvailableIssuedCoupons,
            emptyList(),
        )
    }

    fun filterAvailableCouponList(issuedCoupons: List<IssuedCoupon>): List<IssuedCoupon> {
        return issuedCoupons.filter { it.isAvailableTerm() && !it.usageStatus }
    }

    fun filterExpiredCouponList(issuedCoupons: List<IssuedCoupon>): List<IssuedCoupon> {
        return issuedCoupons.filter { !it.isAvailableTerm() || it.usageStatus }
    }
}
