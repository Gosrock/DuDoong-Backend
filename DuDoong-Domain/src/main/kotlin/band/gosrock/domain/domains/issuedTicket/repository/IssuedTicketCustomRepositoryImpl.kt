package band.gosrock.domain.domains.issuedTicket.repository

import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus.ENTRANCE_COMPLETED
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus.ENTRANCE_INCOMPLETE
import band.gosrock.domain.domains.issuedTicket.domain.QIssuedTicket.issuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.QIssuedTicketOptionAnswer.issuedTicketOptionAnswer
import band.gosrock.domain.domains.issuedTicket.repository.condition.FindEventIssuedTicketsCondition
import band.gosrock.domain.domains.user.domain.QUser.user
import com.querydsl.core.types.ExpressionUtils.count
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import java.util.Optional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class IssuedTicketCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : IssuedTicketCustomRepository {

    override fun searchToPage(condition: FindEventIssuedTicketsCondition, pageable: Pageable): Page<IssuedTicket> {
        val issuedTickets = queryFactory
            .selectFrom(issuedTicket)
            .join(user)
            .on(user.id.eq(issuedTicket.userInfo.userId))
            .where(
                eventIdEq(condition.eventId),
                condition.getSearchStringFilter(),
                issuedTicketStatusNotCanceled()
            )
            .orderBy(issuedTicket.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery: JPAQuery<Long> = queryFactory
            .select(issuedTicket.count())
            .from(issuedTicket)
            .join(user)
            .on(user.id.eq(issuedTicket.userInfo.userId))
            .where(
                eventIdEq(condition.eventId),
                condition.getSearchStringFilter(),
                issuedTicketStatusNotCanceled()
            )

        return PageableExecutionUtils.getPage(issuedTickets, pageable) { countQuery.fetchOne() ?: 0L }
    }

    override fun find(issuedTicketId: Long): Optional<IssuedTicket> {
        val findIssuedTicket = queryFactory
            .selectFrom(issuedTicket)
            .leftJoin(issuedTicket.issuedTicketOptionAnswers, issuedTicketOptionAnswer)
            .fetchJoin()
            .where(issuedTicket.id.eq(issuedTicketId), issuedTicketStatusNotCanceled())
            .fetchOne()
        return Optional.ofNullable(findIssuedTicket)
    }

    override fun countPaidTicket(userId: Long, issuedTicketId: Long): Long {
        val result = queryFactory
            .select(count(issuedTicket))
            .from(issuedTicket)
            .where(eqUserId(userId), eqTicketItemId(issuedTicketId), filterPaidTickets())
            .fetchOne()
        return result ?: 0L
    }

    override fun countIssuedTicketByItemId(ticketItemId: Long): Long {
        val result = queryFactory
            .select(count(issuedTicket))
            .from(issuedTicket)
            .where(eqTicketItemId(ticketItemId), filterPaidTickets())
            .fetchOne()
        return result ?: 0L
    }

    private fun filterPaidTickets(): BooleanExpression =
        issuedTicket.issuedTicketStatus.`in`(ENTRANCE_COMPLETED, ENTRANCE_INCOMPLETE)

    private fun eqTicketItemId(ticketItemId: Long): BooleanExpression =
        issuedTicket.itemInfo.ticketItemId.eq(ticketItemId)

    private fun eqUserId(userId: Long): BooleanExpression =
        issuedTicket.userInfo.userId.eq(userId)

    private fun eventIdEq(eventId: Long?): BooleanExpression? =
        if (eventId == null) null else issuedTicket.eventId.eq(eventId)

    private fun issuedTicketStatusNotCanceled(): BooleanExpression =
        issuedTicket.issuedTicketStatus.eq(IssuedTicketStatus.CANCELED).not()
}
