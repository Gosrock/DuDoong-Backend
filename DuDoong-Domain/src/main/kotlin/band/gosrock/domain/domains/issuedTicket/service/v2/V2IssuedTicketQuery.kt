package band.gosrock.domain.domains.issuedTicket.service.v2

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
 * @property entrance null 이면 유효 티켓 전체. CANCELED 는 목록에서 쓰지 않는다
 * @property searchType 검색어 기준(현재 회원 이름·연락처, v1 발급 티켓 목록과 같음). null 이면 이름
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
