package band.gosrock.domain.domains.coupon.repository;

import static band.gosrock.domain.domains.coupon.domain.QIssuedCoupon.issuedCoupon;

import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IssuedCouponCustomRepositoryImpl implements IssuedCouponCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<IssuedCoupon> findAllByUserIdAndUsageStatusAndCalculateValidTerm(
            Long userId, boolean usageStatus) {
        return queryFactory
                .selectFrom(issuedCoupon)
                // .leftJoin(issuedCoupon.couponCampaign,couponCampaign)
                //                        .fetchJoin()
                .where(userIdEq(userId), usageStatusEq(usageStatus))
                .fetch();
    }

    private BooleanExpression userIdEq(Long userId) {
        return userId == null ? null : issuedCoupon.userId.eq(userId);
    }

    private BooleanExpression usageStatusEq(boolean usageStatus) {
        return issuedCoupon.usageStatus.eq(usageStatus);
    }

    /**
     * 쿠폰 유효 기간 검증 로직 디버깅 후 추가 예정, 현재는 유효기간에 상관 없이 사용안한 해당 유저의 발급받은 모든 쿠폰 노출됨. 각 쿠폰의 사용 가능 마감 시각은
     * 리스폰스로 따로 같이 보내고 있음. *
     */
    //    private BooleanExpression checkValidTerm() {
    //        DateTimePath<LocalDateTime> valid = issuedCoupon.createdAt;
    //        valid.after(issuedCoupon.couponCampaign.validTerm).loe(LocalDateTime.now())
    //
    //
    //
    // LocalDateTime.from((ChronoLocalDateTime<?>)issuedCoupon.createdAt).plusDays(issuedCoupon.couponCampaign.validTerm);
    //
    // LocalDateTime.from((ChronoLocalDateTime<?>)issuedCoupon.createdAt).plusDays(issuedCoupon.couponCampaign.validTerm);
    //        if(validDateTime.isBefore(LocalDateTime.now())){
    //            return null;
    //        }
    //        return null;
    //    }
}
