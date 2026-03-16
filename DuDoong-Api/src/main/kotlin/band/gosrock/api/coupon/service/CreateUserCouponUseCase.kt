package band.gosrock.api.coupon.service

import band.gosrock.api.coupon.dto.response.CreateUserCouponResponse
import band.gosrock.api.coupon.mapper.IssuedCouponMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.coupon.adaptor.CouponCampaignAdaptor
import band.gosrock.domain.domains.coupon.service.CreateIssuedCouponDomainService
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class CreateUserCouponUseCase(
    private val userAdaptor: UserAdaptor,
    private val issuedCouponMapper: IssuedCouponMapper,
    private val couponCampaignAdaptor: CouponCampaignAdaptor,
    private val createIssuedCouponDomainService: CreateIssuedCouponDomainService,
) {
    @Transactional
    fun execute(userId: Long, couponCode: String): CreateUserCouponResponse {
        // 존재하는 유저인지 검증
        val user = userAdaptor.queryUser(userId)
        // 쿠폰 코드 검증
        val couponCampaign = couponCampaignAdaptor.findByCouponCode(couponCode)
        // 재고 감소 및 쿠폰 발급
        val issuedCoupon = createIssuedCouponDomainService.createIssuedCoupon(
            issuedCouponMapper.toEntity(couponCampaign, user.id!!),
            couponCampaign,
        )
        return issuedCouponMapper.toCreateUserCouponResponse(issuedCoupon, couponCampaign)
    }
}
