package band.gosrock.api.statistic.query;

import static band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus.ENTRANCE_COMPLETED;
import static band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus.ENTRANCE_INCOMPLETE;
import static band.gosrock.domain.domains.issuedTicket.domain.QIssuedTicket.issuedTicket;
import static com.querydsl.core.types.ExpressionUtils.count;

import band.gosrock.api.statistic.query.result.IssuedTicketStatistic;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssuedTicketQueryRepository {

    private final JPAQueryFactory queryFactory;

    public IssuedTicketStatistic statistic(Long eventId) {
        return queryFactory
                .select(
                        Projections.constructor(
                                IssuedTicketStatistic.class,
                                issuedCountEx(eventId),
                                enteredCountEx(eventId)))
                .from(issuedTicket)
                .fetchOne();
    }

    private Expression<Long> enteredCountEx(Long eventId) {
        return ExpressionUtils.as(
                JPAExpressions.select(count(issuedTicket.id))
                        .from(issuedTicket)
                        .where(
                                eventIdEq(eventId),
                                issuedTicket.issuedTicketStatus.eq(ENTRANCE_COMPLETED)),
                "enteredCount");
    }

    private Expression<Long> issuedCountEx(Long eventId) {
        return ExpressionUtils.as(
                JPAExpressions.select(count(issuedTicket.id))
                        .from(issuedTicket)
                        .where(
                                eventIdEq(eventId),
                                issuedTicket.issuedTicketStatus.in(
                                        ENTRANCE_COMPLETED, ENTRANCE_INCOMPLETE)),
                "issuedCount");
    }

    private BooleanExpression eventIdEq(Long eventId) {
        return eventId == null ? null : issuedTicket.eventId.eq(eventId);
    }
}
