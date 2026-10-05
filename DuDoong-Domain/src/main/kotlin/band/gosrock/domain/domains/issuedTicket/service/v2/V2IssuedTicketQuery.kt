package band.gosrock.domain.domains.issuedTicket.service.v2

import band.gosrock.domain.domains.gift.domain.QTicketGift.ticketGift
import band.gosrock.domain.domains.gift.domain.TicketGiftStatus
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus
import band.gosrock.domain.domains.issuedTicket.domain.QIssuedTicket.issuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.QIssuedTicketOptionAnswer.issuedTicketOptionAnswer
import band.gosrock.domain.domains.issuedTicket.exception.ExportTooManyIssuedTicketsException
import band.gosrock.domain.domains.order.repository.condition.AdminTableSearchType
import band.gosrock.domain.domains.user.domain.QUser.user
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Component

/** v2 발급 티켓 입장 상태 (#712). 목록·통계는 취소 티켓을 뺀 유효 티켓(BEFORE + DONE) 기준 */
enum class V2EntranceState {
    BEFORE,
    DONE,
    CANCELED;

    companion object {
        fun of(status: IssuedTicketStatus): V2EntranceState = when (status) {
            IssuedTicketStatus.ENTRANCE_INCOMPLETE -> BEFORE
            IssuedTicketStatus.ENTRANCE_COMPLETED -> DONE
            IssuedTicketStatus.CANCELED -> CANCELED
        }
    }
}

/**
 * 호스트 화면(I-1·I-2·I-3)의 선물 상태 (#740). 사용자 앱 T-1 의 giftState 와 달리 보는 사람 기준이 아니라 티켓 기준
 * - PENDING: 선물 링크가 대기 중 (입장 불가 — 현장 스캔 GIFT_PENDING)
 * - ACCEPTED: 선물이 수락되어 소유자가 받은 사람으로 바뀜 (반환되면 다시 NONE)
 */
enum class V2HostGiftState { NONE, PENDING, ACCEPTED }

/**
 * @property entrance null 이면 유효 티켓 전체. CANCELED 는 목록에서 쓰지 않는다
 * @property searchType 검색어 기준(현재 회원 이름·연락처, v1 발급 티켓 목록과 같음). null 이면 이름.
 *   **현재 소유자 기준** — 선물이 수락된 티켓은 받은 사람 이름·연락처로 찾는다(주문자로는 안 찾아짐, #740)
 */
data class V2IssuedTicketSearch(
    val eventId: Long,
    val entrance: V2EntranceState? = null,
    val searchType: AdminTableSearchType? = null,
    val keyword: String? = null,
)

/** 유효 티켓 입장 통계. [entranceRate] 는 입장 완료 / 전체 * 100 (소수 첫째 자리 반올림, 발급 0이면 0) */
data class V2EntranceStats(
    val issuedCount: Long,
    val enteredCount: Long,
) {
    val notEnteredCount: Long get() = issuedCount - enteredCount
    val entranceRate: Double get() = if (issuedCount == 0L) 0.0 else Math.round(enteredCount * 1000.0 / issuedCount) / 10.0
}

@Component
class V2IssuedTicketQuery(private val queryFactory: JPAQueryFactory) {

    fun findPage(search: V2IssuedTicketSearch, pageable: Pageable): Page<IssuedTicket> {
        val content = base(queryFactory.selectFrom(issuedTicket), search, withEntrance = true)
            .orderBy(issuedTicket.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
        val countQuery = base(queryFactory.select(issuedTicket.count()).from(issuedTicket), search, withEntrance = true)
        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    /**
     * 엑셀용 전체 (최신 순, 옵션 답변 fetch join — 행마다 답변을 따로 읽지 않는다). [maxRows] 를 넘으면 조회하지 않고 400 (IssuedTicket_400_7)
     */
    fun findAllForExport(search: V2IssuedTicketSearch, maxRows: Int): List<IssuedTicket> {
        val total = base(queryFactory.select(issuedTicket.count()).from(issuedTicket), search, withEntrance = true).fetchOne() ?: 0L
        if (total > maxRows) throw ExportTooManyIssuedTicketsException.EXCEPTION
        return base(
            queryFactory.selectFrom(issuedTicket).distinct().leftJoin(issuedTicket.issuedTicketOptionAnswers, issuedTicketOptionAnswer).fetchJoin(),
            search,
            withEntrance = true,
        ).orderBy(issuedTicket.id.desc()).fetch()
    }

    /**
     * 티켓별 선물 상태 (한 번의 쿼리, idx_ticket_gift_issued_ticket_id). 대기·수락 선물이 없는 티켓은 결과에 없다(= NONE).
     * 받은 티켓은 다시 선물할 수 없어 둘이 함께 있을 수 없지만, 있으면 대기를 우선한다
     */
    fun giftStatesOf(ticketIds: Collection<Long>): Map<Long, V2HostGiftState> {
        if (ticketIds.isEmpty()) return emptyMap()
        return queryFactory.select(ticketGift.issuedTicketId, ticketGift.status).from(ticketGift)
            .where(ticketGift.issuedTicketId.`in`(ticketIds), ticketGift.status.`in`(TicketGiftStatus.PENDING, TicketGiftStatus.ACCEPTED))
            .fetch()
            .groupBy({ it.get(ticketGift.issuedTicketId)!! }, { it.get(ticketGift.status)!! })
            .mapValues { (_, statuses) ->
                if (TicketGiftStatus.PENDING in statuses) V2HostGiftState.PENDING else V2HostGiftState.ACCEPTED
            }
    }

    /** 검색어를 반영한 입장 상태별 건수 (입장 필터는 무시) */
    fun stats(search: V2IssuedTicketSearch): V2EntranceStats {
        val entered = CaseBuilder().`when`(issuedTicket.issuedTicketStatus.eq(IssuedTicketStatus.ENTRANCE_COMPLETED)).then(1L).otherwise(0L).sum()
        val tuple = base(queryFactory.select(issuedTicket.count(), entered).from(issuedTicket), search, withEntrance = false).fetchOne()
        return V2EntranceStats(issuedCount = tuple?.get(0, Long::class.java) ?: 0L, enteredCount = tuple?.get(entered) ?: 0L)
    }

    private fun <T> base(query: JPAQuery<T>, search: V2IssuedTicketSearch, withEntrance: Boolean): JPAQuery<T> {
        val keyword = search.keyword?.trim()
        val searchFilter = if (keyword.isNullOrEmpty()) null else (search.searchType ?: AdminTableSearchType.NAME).getContains(keyword)
        // 검색할 때만 회원 조인 (v1 목록과 같은 기준). 통계(검색 없음)는 탈퇴 회원 티켓도 센다
        val joined = if (searchFilter == null) query else query.join(user).on(user.id.eq(issuedTicket.userInfo.userId))
        return joined.where(
            issuedTicket.eventId.eq(search.eventId),
            entranceFilter(if (withEntrance) search.entrance else null),
            searchFilter,
        )
    }

    private fun entranceFilter(entrance: V2EntranceState?): BooleanExpression = when (entrance) {
        null -> issuedTicket.issuedTicketStatus.`in`(IssuedTicketStatus.ENTRANCE_INCOMPLETE, IssuedTicketStatus.ENTRANCE_COMPLETED)
        V2EntranceState.BEFORE -> issuedTicket.issuedTicketStatus.eq(IssuedTicketStatus.ENTRANCE_INCOMPLETE)
        V2EntranceState.DONE -> issuedTicket.issuedTicketStatus.eq(IssuedTicketStatus.ENTRANCE_COMPLETED)
        V2EntranceState.CANCELED -> issuedTicket.issuedTicketStatus.eq(IssuedTicketStatus.CANCELED)
    }
}
