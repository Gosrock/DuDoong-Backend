package band.gosrock.api.coupon.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.coupon.dto.reqeust.CreateCouponCampaignRequest;
import band.gosrock.api.coupon.dto.response.CreateCouponCampaignResponse;
import band.gosrock.api.coupon.mapper.CouponCampaignMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.coupon.domain.CouponCampaign;
import band.gosrock.domain.domains.coupon.service.CreateCouponCampaignDomainService;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateCouponUseCase {

    private final UserUtils userUtils;
    private final CreateCouponCampaignDomainService createCouponCampaignDomainService;
    private final HostService hostService;
    private final CouponCampaignMapper couponCampaignMapper;

    @Transactional
    public CreateCouponCampaignResponse execute(
            CreateCouponCampaignRequest createCouponCampaignRequest) {
        // 존재하는 유저인지 검증
        User user = userUtils.getCurrentUser();
        // 슈퍼 호스트인지 검증
        hostService.validateSuperHostUser(createCouponCampaignRequest.getHostId(), user.getId());
        // 이미 생성된 쿠폰 코드인지 검증
        createCouponCampaignDomainService.checkCouponCodeExists(
                createCouponCampaignRequest.getCouponCode());
        // 쿠폰 생성
        CouponCampaign couponCampaign =
                createCouponCampaignDomainService.createCouponCampaign(
                        couponCampaignMapper.toEntity(createCouponCampaignRequest));
        return CouponCampaignMapper.toCreateCouponCampaignResponse(
                couponCampaign, couponCampaign.getHostId());
    }
}
