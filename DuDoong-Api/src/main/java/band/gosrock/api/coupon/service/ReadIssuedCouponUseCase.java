package band.gosrock.api.coupon.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.coupon.dto.response.ReadIssuedCouponResponse;
import band.gosrock.api.coupon.mapper.IssuedCouponMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import band.gosrock.domain.domains.user.domain.User;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ReadIssuedCouponUseCase {
    private final UserUtils userUtils;
    private final IssuedCouponAdaptor issuedCouponAdaptor;
    private final IssuedCouponMapper issuedCouponMapper;

    @Transactional(readOnly = true)
    public ReadIssuedCouponResponse execute(boolean expired) {
        // 존재하는 유저인지 검증
        User user = userUtils.getCurrentUser();

        List<IssuedCoupon> issuedCoupons = issuedCouponAdaptor.findAllByUserId(user.getId());
        // 사용 가능 쿠폰 조회 (사용 이전, 유효 기간 이전)
        List<IssuedCoupon> validTermAvailableIssuedCoupons =
                issuedCoupons.stream()
                        .filter(
                                issuedCoupon ->
                                        issuedCoupon.isAvailableTerm()
                                                && !issuedCoupon.getUsageStatus())
                        .toList();

        // 만료된 쿠폰 조회(사용 완료, 유효 기간 만료)
        if (expired) {
            List<IssuedCoupon> expiredValidTermIssuedCoupons =
                    issuedCoupons.stream()
                            .filter(
                                    issuedCoupon ->
                                            !issuedCoupon.isAvailableTerm()
                                                    || issuedCoupon.getUsageStatus())
                            .toList();
            return issuedCouponMapper.toReadIssuedCouponMyPageResponse(
                    validTermAvailableIssuedCoupons, expiredValidTermIssuedCoupons);
        }

        return issuedCouponMapper.toReadIssuedCouponMyPageResponse(
                validTermAvailableIssuedCoupons, new ArrayList<>());
    }
}
