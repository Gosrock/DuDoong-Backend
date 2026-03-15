package band.gosrock.api.coupon.service

import band.gosrock.api.common.UserUtils
import band.gosrock.api.coupon.dto.reqeust.CreateCouponCampaignRequest
import band.gosrock.api.coupon.dto.response.CreateCouponCampaignResponse
import band.gosrock.api.coupon.mapper.CouponCampaignMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.coupon.service.CreateCouponCampaignDomainService
import band.gosrock.domain.domains.host.service.HostService
import org.springframework.transaction.annotation.Transactional

@UseCase
class CreateCouponUseCase(
    private val userUtils: UserUtils,
    private val createCouponCampaignDomainService: CreateCouponCampaignDomainService,
    private val hostService: HostService,
    private val couponCampaignMapper: CouponCampaignMapper,
) {
    @Transactional
    fun execute(createCouponCampaignRequest: CreateCouponCampaignRequest): CreateCouponCampaignResponse {
        // 존재하는 유저인지 검증
        val user = userUtils.getCurrentUser()
        // 이미 생성된 쿠폰 코드인지 검증
        createCouponCampaignDomainService.checkCouponCodeExists(createCouponCampaignRequest.couponCode)
        // 쿠폰 생성
        val couponCampaign = createCouponCampaignDomainService.createCouponCampaign(
            couponCampaignMapper.toEntity(createCouponCampaignRequest, user.id!!),
        )
        return CouponCampaignMapper.toCreateCouponCampaignResponse(couponCampaign, user.id!!)
    }
}
