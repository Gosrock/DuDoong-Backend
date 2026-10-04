package band.gosrock.domain.domains.host.service.v2

import band.gosrock.domain.domains.event.domain.QEvent.event
import band.gosrock.domain.domains.event.service.v2.V2EventBrowseDomainService
import band.gosrock.domain.domains.event.service.v2.V2EventBrowseQuery
import band.gosrock.domain.domains.event.service.v2.V2EventSummaryRow
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.QHost.host
import band.gosrock.domain.domains.host.domain.QHostFollow.hostFollow
import band.gosrock.domain.domains.host.domain.QHostUser.hostUser
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Component

/** 마이페이지(M-1) 소속 호스트 요약 */
data class V2MyHostSummaryRow(
    val hostId: Long,
    val name: String?,
    val profileImageKey: String?,
    val role: HostRole,
)

/** 관심 호스트(M-4) 한 행 */
data class V2FollowingHostRow(
    val hostId: Long,
    val name: String?,
    val profileImageKey: String?,
)

/**
 * 관심 호스트 '전체 상태' 필터 (#729). 대표 공연([band.gosrock.domain.domains.event.service.v2.V2RepresentativeEventRule])과 같은 기준
 * - ACTIVE: 진행 중·예정 공개 공연이 있는 호스트 (대표 공연이 ONGOING·UPCOMING)
 * - ENDED: 진행 중·예정 공연은 없고 끝난 공개 공연만 있는 호스트 (대표 공연이 PAST)
 * 공개 공연이 하나도 없는 호스트는 ALL 에만 나온다
 */
enum class V2FollowingHostFilter { ALL, ACTIVE, ENDED }

/**
 * v2 마이페이지 호스트 조회 (#729). 호스트 엔티티는 멤버(`Host.hostUsers`)를 EAGER 로 읽으므로 모두 스칼라 조회한다
 */
@Component
class V2MyPageHostQuery(private val queryFactory: JPAQueryFactory) {

    /** M-1: 내가 속한(활성) 호스트 — 합류 최신순(host_user 생성 시각 → id 내림차순) 최대 [limit]개 */
    fun findMyHosts(userId: Long, limit: Int): List<V2MyHostSummaryRow> =
        queryFactory.select(host.id, host.profile.name, host.profile.profileImage.imageKey, hostUser.role)
            .from(hostUser)
            .join(hostUser.host, host)
            .where(hostUser.userId.eq(userId), hostUser.active.isTrue)
            .orderBy(hostUser.createdAt.desc(), hostUser.id.desc())
            .limit(limit.toLong())
            .fetch()
            .map {
                V2MyHostSummaryRow(
                    hostId = it.get(host.id)!!,
                    name = it.get(host.profile.name),
                    profileImageKey = it.get(host.profile.profileImage.imageKey),
                    role = it.get(hostUser.role)!!,
                )
            }

    fun countMyHosts(userId: Long): Long =
        queryFactory.select(hostUser.count()).from(hostUser)
            .where(hostUser.userId.eq(userId), hostUser.active.isTrue)
            .fetchOne() ?: 0L

    /** M-4: 내가 팔로우한 호스트 — 최근 팔로우 순(host_follow_id 내림차순, idx_host_follow_user_id) */
    fun findFollowingHosts(userId: Long, filter: V2FollowingHostFilter, now: LocalDateTime, pageable: Pageable): Page<V2FollowingHostRow> {
        val condition = arrayOf(hostFollow.userId.eq(userId), filterCondition(filter, now))
        val content = queryFactory.select(host.id, host.profile.name, host.profile.profileImage.imageKey)
            .from(hostFollow)
            .join(host).on(host.id.eq(hostFollow.hostId))
            .where(*condition)
            .orderBy(hostFollow.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
            .map {
                V2FollowingHostRow(
                    hostId = it.get(host.id)!!,
                    name = it.get(host.profile.name),
                    profileImageKey = it.get(host.profile.profileImage.imageKey),
                )
            }
        val countQuery = queryFactory.select(hostFollow.count())
            .from(hostFollow)
            .join(host).on(host.id.eq(hostFollow.hostId))
            .where(*condition)
        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    /** 호스트들의 공개 공연 (한 번의 스칼라 쿼리). 대표 공연은 [band.gosrock.domain.domains.event.service.v2.V2RepresentativeEventRule] 로 고른다 */
    fun findPublicEventsByHostIds(hostIds: Collection<Long>): List<V2EventSummaryRow> {
        if (hostIds.isEmpty()) return emptyList()
        return queryFactory
            .select(event.id, event.hostId, event.eventBasic.name, event.eventDetail.posterImage.imageKey, event.status, event.eventBasic.startAt, event.eventBasic.runTime)
            .from(event)
            .where(event.hostId.`in`(hostIds), event.status.`in`(PUBLIC_STATUSES))
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
    }

    private fun filterCondition(filter: V2FollowingHostFilter, now: LocalDateTime): BooleanExpression? =
        when (filter) {
            V2FollowingHostFilter.ALL -> null
            V2FollowingHostFilter.ACTIVE -> hasActiveEvent(now)
            V2FollowingHostFilter.ENDED -> hasActiveEvent(now).not().and(hasPublicEvent())
        }

    private fun hasActiveEvent(now: LocalDateTime): BooleanExpression =
        JPAExpressions.selectOne().from(event)
            .where(event.hostId.eq(hostFollow.hostId), V2EventBrowseQuery.activeCondition(now))
            .exists()

    private fun hasPublicEvent(): BooleanExpression =
        JPAExpressions.selectOne().from(event)
            .where(event.hostId.eq(hostFollow.hostId), event.status.`in`(PUBLIC_STATUSES))
            .exists()

    companion object {
        private val PUBLIC_STATUSES = V2EventBrowseDomainService.PUBLIC_STATUSES.toList()
    }
}
