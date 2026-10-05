package band.gosrock.api.coupon.service

import band.gosrock.api.coupon.dto.response.CreateUserCouponResponse
import band.gosrock.api.coupon.mapper.IssuedCouponMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.coupon.adaptor.CouponCampaignAdaptor
import band.gosrock.domain.domains.coupon.service.CreateIssuedCouponDomainService
import band.gosrock.domain.domains.user.adaptor.UserAdaptor

@UseCase
class CreateUserCouponUseCase(
    private val userAdaptor: UserAdaptor,
    private val issuedCouponMapper: IssuedCouponMapper,
    private val couponCampaignAdaptor: CouponCampaignAdaptor,
    private val createIssuedCouponDomainService: CreateIssuedCouponDomainService,
) {
    /**
     * 트랜잭션 없이 부른다 (#746): 트랜잭션이 있으면 그 커넥션을 쥔 채 발급 락을 기다리고(#743 후속), 락 밖에서 읽은 캠페인이 커밋 때 함께 저장된다.
     * 재고 감소·발급은 도메인 서비스의 락 트랜잭션에서 캠페인을 다시 읽어 한다
     */
    fun execute(userId: Long, couponCode: String): CreateUserCouponResponse {
        // 존재하는 유저인지 검증
        val user = userAdaptor.queryUser(userId)
        // 쿠폰 코드 검증 (락 키용 id 만 쓴다)
        val couponCampaignId = couponCampaignAdaptor.findByCouponCode(couponCode).id!!
        // 재고 감소 및 쿠폰 발급
        val issuedCoupon = createIssuedCouponDomainService.createIssuedCoupon(user.id!!, couponCampaignId)
        return issuedCouponMapper.toCreateUserCouponResponse(issuedCoupon, issuedCoupon.couponCampaign!!)
    }
}
