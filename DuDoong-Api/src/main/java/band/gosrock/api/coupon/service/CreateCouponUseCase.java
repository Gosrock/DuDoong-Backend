package band.gosrock.api.coupon.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.coupon.dto.reqeust.CreateCouponCampaignRequest;
import band.gosrock.api.coupon.dto.response.CreateCouponCampaignResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.coupon.domain.CouponCampaign;
import band.gosrock.domain.domains.coupon.service.CreateCouponCampaignDomainService;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateCouponUseCase {

    private final UserDomainService userDomainService;
    private final CreateCouponCampaignDomainService createCouponCampaignDomainService;

    @Transactional
    public CreateCouponCampaignResponse execute(
            CreateCouponCampaignRequest createCouponCampaignRequest) {
        // 존재하는 유저인지 검증
        userDomainService.retrieveUser(SecurityUtils.getCurrentUserId());
        // 슈퍼 호스트인지 검증
        // TODO : 차후 호스트쪽 코드 구조 나오는 거 보고 넣을 예정
        // 이미 생성된 쿠폰 코드인지 검증
        createCouponCampaignDomainService.checkCouponCodeExists(
                createCouponCampaignRequest.getCouponCode());
        // 쿠폰 생성
        CouponCampaign couponCampaign =
                createCouponCampaignDomainService.createCouponCampaign(
                        createCouponCampaignRequest.toOnceEntity());
        return CreateCouponCampaignResponse.of(couponCampaign, couponCampaign.getHostId());
    }
}
