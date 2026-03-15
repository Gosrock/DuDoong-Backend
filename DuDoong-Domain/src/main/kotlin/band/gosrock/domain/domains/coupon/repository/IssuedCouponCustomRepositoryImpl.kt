package band.gosrock.domain.domains.coupon.repository

import band.gosrock.domain.domains.coupon.domain.IssuedCoupon
import band.gosrock.domain.domains.coupon.domain.QIssuedCoupon.issuedCoupon
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory

class IssuedCouponCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : IssuedCouponCustomRepository {

    override fun findAllByUserId(userId: Long): List<IssuedCoupon> =
        queryFactory.selectFrom(issuedCoupon).where(userIdEq(userId)).fetch()

    private fun userIdEq(userId: Long?): BooleanExpression? =
        if (userId == null) null else issuedCoupon.userId.eq(userId)
}
