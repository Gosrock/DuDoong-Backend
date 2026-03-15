package band.gosrock.api.statistic.query

import band.gosrock.api.statistic.query.result.IssuedTicketStatistic
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus.ENTRANCE_COMPLETED
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus.ENTRANCE_INCOMPLETE
import band.gosrock.domain.domains.issuedTicket.domain.QIssuedTicket.issuedTicket
import com.querydsl.core.types.Expression
import com.querydsl.core.types.ExpressionUtils
import com.querydsl.core.types.ExpressionUtils.count
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Component

@Component
class IssuedTicketQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {
    fun statistic(eventId: Long): IssuedTicketStatistic =
        queryFactory
            .select(
                Projections.constructor(
                    IssuedTicketStatistic::class.java,
                    issuedCountEx(eventId),
                    enteredCountEx(eventId),
                ),
            )
            .from(issuedTicket)
            .fetchFirst()

    private fun enteredCountEx(eventId: Long): Expression<Long> =
        ExpressionUtils.`as`(
            JPAExpressions.select(count(issuedTicket.id))
                .from(issuedTicket)
                .where(
                    eventIdEq(eventId),
                    issuedTicket.issuedTicketStatus.eq(ENTRANCE_COMPLETED),
                ),
            "enteredCount",
        )

    private fun issuedCountEx(eventId: Long): Expression<Long> =
        ExpressionUtils.`as`(
            JPAExpressions.select(count(issuedTicket.id))
                .from(issuedTicket)
                .where(
                    eventIdEq(eventId),
                    issuedTicket.issuedTicketStatus.`in`(ENTRANCE_COMPLETED, ENTRANCE_INCOMPLETE),
                ),
            "issuedCount",
        )

    private fun eventIdEq(eventId: Long?): BooleanExpression? =
        if (eventId == null) null else issuedTicket.eventId.eq(eventId)
}
