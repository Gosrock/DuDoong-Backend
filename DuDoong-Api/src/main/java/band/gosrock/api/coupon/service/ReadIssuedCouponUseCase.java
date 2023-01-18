package band.gosrock.api.coupon.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.coupon.dto.response.ReadIssuedCouponOrderResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ReadIssuedCouponUseCase {

    private final UserDomainService userDomainService;
    private final IssuedCouponAdaptor issuedCouponAdaptor;

    /**
     * 주문시 쿠폰 조회 API
     *
     * @return ReadIssuedCouponResponse
     */
    @Transactional(readOnly = true)
    public ReadIssuedCouponOrderResponse execute() {
        // TODO : utils로 변경 필요
        Long currentUserId = SecurityUtils.getCurrentUserId();
        // 존재하는 유저인지 검증
        userDomainService.retrieveUser(currentUserId);
        return ReadIssuedCouponOrderResponse.of(
                issuedCouponAdaptor.findAllByUserIdAndUsageStatusAndValidTerm(currentUserId));
    }
}
