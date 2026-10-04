package band.gosrock.domain.domains.event.service.v2

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.domain.QEvent.event
import band.gosrock.domain.domains.event.domain.QEventTag.eventTag
import band.gosrock.domain.domains.host.domain.QHost.host
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Component

/**
 * 공개 공연 리스트(P-2) 검색 조건 (#716).
 * @property keyword 공연명 OR 호스트명 부분일치 (대소문자 무시, `%`·`_` 는 문자 그대로 — QueryDSL contains 가 이스케이프)
 * @property tagIdGroups 분류별 태그 id 묶음. 묶음 안은 OR, 묶음끼리는 AND ([V2EventBrowseDomainService.tagFilterGroups])
 * @property includePast false 면 다가오는 공연(OPEN + 시작 전)만, true 면 OPEN(시작한 것 포함) + CALCULATING + CLOSED
 */
data class V2EventBrowseSearch(
    val keyword: String? = null,
    val tagIdGroups: List<List<Long>> = emptyList(),
    val includePast: Boolean = false,
)

/** v2 사용자 앱 공연 탐색 쿼리 (홈 캐러셀·공연 리스트). 준비중·삭제 공연은 절대 포함하지 않는다. v2 에서만 쓴다 */
@Component
class V2EventBrowseQuery(private val queryFactory: JPAQueryFactory) {

    /** P-1: 등록(OPEN)·시작 전 공연, 시작 임박순 (같으면 id 순) 최대 [limit]개 */
    fun findUpcoming(now: LocalDateTime, limit: Int): List<Event> =
        queryFactory.selectFrom(event)
            .where(upcoming(now))
            .orderBy(event.eventBasic.startAt.asc(), event.id.asc())
            .limit(limit.toLong())
            .fetch()

    /**
     * P-2 공연 리스트 (정렬 UPCOMING).
     * 1그룹 = 다가오는 공연(OPEN + 시작 전) 시작 임박순, 2그룹 = 지난 공연(시작이 지난 OPEN, CALCULATING, CLOSED) 최근 시작 순.
     * 같은 시작 시각은 id 순. 표시 상태([V2EventBrowseDomainService.displayStatusOf])와 같은 기준
     */
    fun search(search: V2EventBrowseSearch, now: LocalDateTime, pageable: Pageable): Page<Event> {
        val upcoming = upcoming(now)
        val content = base(queryFactory.selectFrom(event), search, now)
            .orderBy(
                CaseBuilder().`when`(upcoming).then(0).otherwise(1).asc(),
                CaseBuilder().`when`(upcoming).then(event.eventBasic.startAt)
                    .otherwise(Expressions.nullExpression(LocalDateTime::class.java)).asc(),
                event.eventBasic.startAt.desc(),
                event.id.asc(),
            )
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
        val countQuery = base(queryFactory.select(event.count()).from(event), search, now)
        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    /** 공연별 태그 id (한 번의 쿼리). 태그가 없는 공연은 결과에 없음 */
    fun findTagIdsByEventIds(eventIds: Collection<Long>): Map<Long, List<Long>> {
        if (eventIds.isEmpty()) return emptyMap()
        return queryFactory.select(eventTag.event.id, eventTag.tagId).from(eventTag)
            .where(eventTag.event.id.`in`(eventIds))
            .orderBy(eventTag.id.asc())
            .fetch()
            .groupBy({ it.get(eventTag.event.id)!! }, { it.get(eventTag.tagId)!! })
    }

    /** 호스트 이름 (한 번의 스칼라 쿼리 — 호스트 엔티티의 멤버 EAGER 로딩을 피한다) */
    fun findHostNames(hostIds: Collection<Long>): Map<Long, String?> {
        if (hostIds.isEmpty()) return emptyMap()
        return queryFactory.select(host.id, host.profile.name).from(host)
            .where(host.id.`in`(hostIds))
            .fetch()
            .associate { it.get(host.id)!! to it.get(host.profile.name) }
    }

    private fun <T> base(query: JPAQuery<T>, search: V2EventBrowseSearch, now: LocalDateTime): JPAQuery<T> {
        val keyword = search.keyword?.trim()?.ifEmpty { null }
        // 검색어가 있을 때만 호스트 조인 (PK eq_ref)
        val joined = if (keyword == null) query else query.leftJoin(host).on(host.id.eq(event.hostId))
        return joined.where(
            if (search.includePast) event.status.`in`(PUBLIC_STATUSES) else upcoming(now),
            keyword?.let { event.eventBasic.name.containsIgnoreCase(it).or(host.profile.name.containsIgnoreCase(it)) },
            *search.tagIdGroups.map { hasAnyTag(it) }.toTypedArray(),
        )
    }

    /** 다가오는 공연 = 등록(OPEN) + 시작 전. 표시 상태 UPCOMING 과 같은 기준 */
    private fun upcoming(now: LocalDateTime): BooleanExpression =
        event.status.eq(EventStatus.OPEN).and(event.eventBasic.startAt.gt(now))

    /** 태그 묶음 중 하나라도 붙은 공연 (EXISTS, uk(event_id, tag_id) 사용) */
    private fun hasAnyTag(tagIds: List<Long>): BooleanExpression =
        JPAExpressions.selectOne().from(eventTag)
            .where(eventTag.event.id.eq(event.id), eventTag.tagId.`in`(tagIds))
            .exists()

    companion object {
        private val PUBLIC_STATUSES = V2EventBrowseDomainService.PUBLIC_STATUSES.toList()
    }
}
