package band.gosrock.domain.domains.issuedTicket.service.v2

import band.gosrock.domain.domains.event.domain.QEvent.event
import band.gosrock.domain.domains.event.service.v2.V2EventConditions
import band.gosrock.domain.domains.event.service.v2.V2EventSummaryRow
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus
import band.gosrock.domain.domains.issuedTicket.domain.QIssuedTicket.issuedTicket
import band.gosrock.domain.domains.order.domain.QOrder.order
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Component

/**
 * v2 관람 공연 아카이빙(M-5) 조회 (#729, DEC-022 #5): 내가 입장한 지난 공연.
 *
 * - 입장 기록 = **현재 소유자**(`tbl_issued_ticket.user_id`)가 나이고 입장 완료(ENTRANCE_COMPLETED)인 발급 티켓.
 *   선물로 받아 입장한 티켓은 소유자가 받은 사람으로 바뀌므로 자동으로 포함되고, 보낸 사람(주문자)에게서는 빠진다.
 *   입장 전·취소 티켓은 제외 (입장 후 취소된 티켓도 상태가 CANCELED 라 제외)
 * - 지난 공연 = 표시 상태 PAST 인 공개 공연 ([V2EventConditions.ended]): CALCULATING·CLOSED, 종료된 OPEN, startAt 이 없는 OPEN(방어 — 표시 규칙과 같이 PAST).
 *   준비중·삭제 공연은 제외. startAt 이 없는 공연은 연도 탭·연도 필터에는 안 나오고 전체(year 없음)의 맨 뒤에 나온다
 * - 공연 단위로 한 번만 (같은 공연 여러 장 입장해도 1건). 공연 시작 최근 순, 같으면 id 큰 순
 * - 연도 = 공연 시작 연도 (start_at 범위 조건)
 *
 * 입장 기록 서브쿼리는 user_id 선두 인덱스(`idx_issued_ticket_user_id_id`, #719 V008)를 쓴다 — #719 이후 머지
 */
@Component
class V2ArchiveQuery(private val queryFactory: JPAQueryFactory) {

    fun findArchivedEvents(userId: Long, year: Int?, now: LocalDateTime, pageable: Pageable): Page<V2EventSummaryRow> {
        val condition = arrayOf(enteredBy(userId), V2EventConditions.ended(now), year?.let { inYear(it) })
        val content = queryFactory
            .select(event.id, event.hostId, event.eventBasic.name, event.eventDetail.posterImage.imageKey, event.status, event.eventBasic.startAt, event.eventBasic.runTime)
            .from(event)
            .where(*condition)
            .orderBy(event.eventBasic.startAt.desc().nullsLast(), event.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
            .map {
                V2EventSummaryRow(
                    eventId = it.get(event.id)!!,
                    hostId = it.get(event.hostId)!!,
                    name = it.get(event.eventBasic.name),
                    posterImageKey = it.get(event.eventDetail.posterImage.imageKey),
                    status = it.get(event.status)!!,
                    startAt = it.get(event.eventBasic.startAt),
                    runTime = it.get(event.eventBasic.runTime),
                )
            }
        val countQuery = queryFactory.select(event.count()).from(event).where(*condition)
        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    /** 연도 탭: 아카이빙 공연이 있는 공연 시작 연도 (최근 순) */
    fun findArchivedYears(userId: Long, now: LocalDateTime): List<Int> =
        queryFactory.select(event.eventBasic.startAt.year()).distinct()
            .from(event)
            .where(enteredBy(userId), V2EventConditions.ended(now), event.eventBasic.startAt.isNotNull)
            .fetch()
            .filterNotNull()
            .sortedDescending()

    /**
     * 공연별로 내가 주문한 주문 중 가장 최근에 입장 티켓이 발급된 주문 uuid (주문상세 이동용, 한 번의 쿼리).
     * 선물받은 티켓만 입장한 공연은 주문이 보낸 사람 것이라 결과에 없다 (O-3 은 본인 주문만)
     */
    fun findMyOrderUuids(userId: Long, eventIds: Collection<Long>): Map<Long, String> {
        if (eventIds.isEmpty()) return emptyMap()
        return queryFactory.select(issuedTicket.eventId, issuedTicket.orderUuid)
            .from(issuedTicket)
            .join(order).on(order.uuid.eq(issuedTicket.orderUuid))
            .where(
                issuedTicket.userInfo.userId.eq(userId),
                issuedTicket.issuedTicketStatus.eq(IssuedTicketStatus.ENTRANCE_COMPLETED),
                issuedTicket.eventId.`in`(eventIds),
                order.userId.eq(userId),
            )
            .orderBy(issuedTicket.id.desc())
            .fetch()
            .groupBy { it.get(issuedTicket.eventId)!! }
            .mapValues { (_, rows) -> rows.first().get(issuedTicket.orderUuid)!! }
    }

    private fun enteredBy(userId: Long): BooleanExpression =
        event.id.`in`(
            JPAExpressions.select(issuedTicket.eventId).from(issuedTicket)
                .where(
                    issuedTicket.userInfo.userId.eq(userId),
                    issuedTicket.issuedTicketStatus.eq(IssuedTicketStatus.ENTRANCE_COMPLETED),
                ),
        )

    private fun inYear(year: Int): BooleanExpression {
        val from = LocalDateTime.of(year, 1, 1, 0, 0)
        return event.eventBasic.startAt.goe(from).and(event.eventBasic.startAt.lt(from.plusYears(1)))
    }
}
