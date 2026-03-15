package band.gosrock.api.statistic.query

import band.gosrock.api.statistic.query.result.OrderStatistic
import band.gosrock.domain.domains.order.domain.OrderStatus.APPROVED
import band.gosrock.domain.domains.order.domain.OrderStatus.CONFIRM
import band.gosrock.domain.domains.order.domain.OrderStatus.PENDING_APPROVE
import band.gosrock.domain.domains.order.domain.QOrder.order
import com.querydsl.core.types.Expression
import com.querydsl.core.types.ExpressionUtils
import com.querydsl.core.types.ExpressionUtils.count
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import java.math.BigDecimal
import org.springframework.stereotype.Component

@Component
class OrderQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {
    fun statistic(eventId: Long): OrderStatistic =
        queryFactory
            .select(
                Projections.constructor(
                    OrderStatistic::class.java,
                    doneCountEx(eventId),
                    notApprovedCountEx(eventId),
                    sellAmount(eventId),
                ),
            )
            .from(order)
            .fetchFirst()

    private fun doneCountEx(eventId: Long): Expression<Long> =
        ExpressionUtils.`as`(
            JPAExpressions.select(count(order.id))
                .from(order)
                .where(eventIdEq(eventId), order.orderStatus.`in`(CONFIRM, APPROVED)),
            "doneCount",
        )

    private fun notApprovedCountEx(eventId: Long): Expression<Long> =
        ExpressionUtils.`as`(
            JPAExpressions.select(count(order.id))
                .from(order)
                .where(eventIdEq(eventId), order.orderStatus.eq(PENDING_APPROVE)),
            "notApprovedCount",
        )

    private fun sellAmount(eventId: Long): Expression<BigDecimal> =
        ExpressionUtils.`as`(
            JPAExpressions.select(order.totalPaymentInfo.paymentAmount.amount.sum())
                .from(order)
                .where(eventIdEq(eventId), order.orderStatus.`in`(CONFIRM, APPROVED)),
            "sellAmount",
        )

    private fun eventIdEq(eventId: Long?): BooleanExpression? =
        if (eventId == null) null else order.eventId.eq(eventId)
}
