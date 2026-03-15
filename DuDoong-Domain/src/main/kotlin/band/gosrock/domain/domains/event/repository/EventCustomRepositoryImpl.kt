package band.gosrock.domain.domains.event.repository

import band.gosrock.domain.common.util.SliceUtil
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.domain.EventStatus.CLOSED
import band.gosrock.domain.domains.event.domain.EventStatus.OPEN
import band.gosrock.domain.domains.event.domain.QEvent.event
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.DateTemplate
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDateTime
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

class EventCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : EventCustomRepository {

    override fun querySliceEventsByHostIdIn(hostIds: List<Long>, pageable: Pageable): Slice<Event> {
        val events = queryFactory
            .selectFrom(event)
            .where(hostIdIn(hostIds))
            .orderBy(statusDesc(), createdAtDesc())
            .offset(pageable.offset)
            .limit(pageable.pageSize + 1L)
            .fetch()
        return SliceUtil.valueOf(events, pageable)
    }

    override fun querySliceEventsByStatus(status: EventStatus, pageable: Pageable): Slice<Event> {
        val events = queryFactory
            .selectFrom(event)
            .where(statusEq(status))
            .orderBy(statusDesc(), startAtAsc())
            .offset(pageable.offset)
            .limit(pageable.pageSize + 1L)
            .fetch()
        return SliceUtil.valueOf(events, pageable)
    }

    override fun querySliceEventsByKeyword(keyword: String, pageable: Pageable): Slice<Event> {
        val openEvents = queryFactory
            .selectFrom(event)
            .where(eqStatusOpen().and(nameContains(keyword)))
            .orderBy(createdAtAsc())
            .offset(pageable.offset)
            .limit(pageable.pageSize + 1L)
            .fetch()

        val remainingSize = pageable.pageSize - openEvents.size
        if (remainingSize >= 0) {
            openEvents.addAll(queryClosedEventsByKeywordAndSize(keyword, pageable, remainingSize.toLong()))
        }
        return SliceUtil.valueOf(openEvents, pageable)
    }

    override fun queryEventsByEndAtBeforeAndStatusOpen(time: LocalDateTime): List<Event> =
        queryFactory.selectFrom(event).where(endAtBefore(time), statusEq(OPEN)).fetch()

    private fun queryClosedEventsByKeywordAndSize(keyword: String, pageable: Pageable, size: Long): List<Event> {
        val totalOpenEventsSize = queryCountByKeywordAndStatus(keyword, OPEN)
        val closedEventsOffset = maxOf(pageable.offset - totalOpenEventsSize, 0L)
        return queryFactory
            .selectFrom(event)
            .where(eqStatusClosed().and(nameContains(keyword)))
            .orderBy(startAtDesc())
            .offset(closedEventsOffset)
            .limit(size + 1)
            .fetch()
    }

    private fun queryCountByKeywordAndStatus(keyword: String, status: EventStatus): Long =
        queryFactory
            .from(event)
            .where(statusEq(status).and(nameContains(keyword)))
            .fetchCount()

    private fun hostIdIn(hostIds: List<Long>): BooleanExpression =
        event.hostId.`in`(hostIds)

    private fun eqStatusOpen(): BooleanExpression = event.status.eq(OPEN)

    private fun eqStatusClosed(): BooleanExpression = event.status.eq(CLOSED)

    private fun statusEq(status: EventStatus): BooleanExpression = event.status.eq(status)

    private fun nameContains(keyword: String?): BooleanExpression? =
        if (keyword == null) null else event.eventBasic.name.containsIgnoreCase(keyword)

    private fun createdAtDesc(): OrderSpecifier<LocalDateTime> = event.createdAt.desc()

    private fun createdAtAsc(): OrderSpecifier<LocalDateTime> = event.createdAt.asc()

    private fun startAtAsc(): OrderSpecifier<LocalDateTime> = event.eventBasic.startAt.asc()

    private fun startAtDesc(): OrderSpecifier<LocalDateTime> = event.eventBasic.startAt.desc()

    private fun statusDesc(): OrderSpecifier<EventStatus> = event.status.desc()

    private fun endAtBefore(time: LocalDateTime): BooleanExpression {
        val eventEndAtTemplate: DateTemplate<LocalDateTime> = Expressions.dateTemplate(
            LocalDateTime::class.java,
            "TIMESTAMPADD(MINUTE,{0}, {1}) ",
            event.eventBasic.runTime,
            event.eventBasic.startAt
        )
        return eventEndAtTemplate.before(time)
    }
}
