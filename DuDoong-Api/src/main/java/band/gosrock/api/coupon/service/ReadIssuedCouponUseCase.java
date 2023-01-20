package band.gosrock.api.coupon.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.coupon.dto.response.ReadIssuedCouponOrderResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import band.gosrock.domain.domains.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ReadIssuedCouponUseCase {

    private final UserUtils userUtils;
    private final IssuedCouponAdaptor issuedCouponAdaptor;

    /**
     * 주문시 쿠폰 조회 API
     *
     * @return ReadIssuedCouponResponse
     */
    @Transactional(readOnly = true)
    public ReadIssuedCouponOrderResponse execute() {
        // 존재하는 유저인지 검증
        User user = userUtils.getCurrentUser();

        List<IssuedCoupon> issuedCoupons =
                issuedCouponAdaptor.findAllByUserIdAndUsageStatus(user.getId());

        List<IssuedCoupon> validTermIssuedCoupons =
                issuedCoupons.stream()
                        .filter(
                                issuedCoupon ->
                                        !LocalDateTime.now()
                                                .isAfter(
                                                        issuedCoupon
                                                                .getCreatedAt()
                                                                .plusDays(
                                                                        issuedCoupon
                                                                                .getCouponCampaign()
                                                                                .getValidTerm())))
                        .toList();
        return ReadIssuedCouponOrderResponse.of(validTermIssuedCoupons);
    }
}
