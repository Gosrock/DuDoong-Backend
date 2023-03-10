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
    public List<IssuedCoupon> findAllByUserId(Long userId) {
        return queryFactory.selectFrom(issuedCoupon).where(userIdEq(userId)).fetch();
    }

    private BooleanExpression userIdEq(Long userId) {
        return userId == null ? null : issuedCoupon.userId.eq(userId);
    }
}
