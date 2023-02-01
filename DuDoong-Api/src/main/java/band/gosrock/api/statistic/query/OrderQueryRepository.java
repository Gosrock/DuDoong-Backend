package band.gosrock.api.statistic.query;

import static band.gosrock.domain.domains.order.domain.OrderStatus.APPROVED;
import static band.gosrock.domain.domains.order.domain.OrderStatus.CONFIRM;
import static band.gosrock.domain.domains.order.domain.OrderStatus.PENDING_APPROVE;
import static band.gosrock.domain.domains.order.domain.QOrder.order;
import static com.querydsl.core.types.ExpressionUtils.count;

import band.gosrock.api.statistic.query.result.OrderStatistic;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final JPAQueryFactory queryFactory;

    private Expression<Long> doneCountEx(Long eventId) {
        return ExpressionUtils.as(
                JPAExpressions.select(count(order.id))
                        .from(order)
                        .where(eventIdEq(eventId), order.orderStatus.in(CONFIRM, APPROVED)),
                "doneCount");
    }

    private Expression<Long> notApprovedCountEx(Long eventId) {
        return ExpressionUtils.as(
                JPAExpressions.select(count(order.id))
                        .from(order)
                        .where(eventIdEq(eventId), order.orderStatus.eq(PENDING_APPROVE)),
                "notApprovedCount");
    }

    private Expression<BigDecimal> sellAmount(Long eventId) {
        return ExpressionUtils.as(
                JPAExpressions.select(order.totalPaymentInfo.paymentAmount.amount.sum())
                        .from(order)
                        .where(eventIdEq(eventId), order.orderStatus.eq(CONFIRM)),
                "sellAmount");
    }

    public OrderStatistic statistic(Long eventId) {

        return queryFactory
                .select(
                        Projections.constructor(
                                OrderStatistic.class,
                                doneCountEx(eventId),
                                notApprovedCountEx(eventId),
                                sellAmount(eventId)))
                .from(order)
                .fetchFirst();
    }

    private BooleanExpression eventIdEq(Long eventId) {
        return eventId == null ? null : order.eventId.eq(eventId);
    }
}
