package band.gosrock.domain.domains.event.service.v2

import band.gosrock.domain.domains.event.domain.EventStatus
import java.time.LocalDateTime

/** 목록 표시용 공연 요약 (스칼라 조회 — 엔티티 로딩 없이). 종료 시각 계산은 [band.gosrock.domain.domains.event.domain.EventBasic.endAt] 과 같다 */
data class V2EventSummaryRow(
    val eventId: Long,
    val hostId: Long,
    val name: String?,
    val posterImageKey: String?,
    val status: EventStatus,
    val startAt: LocalDateTime?,
    val runTime: Long?,
) {
    val endAt: LocalDateTime? get() = if (runTime == null) null else startAt?.plusMinutes(runTime)

    fun displayStatus(now: LocalDateTime): V2EventDisplayStatus = V2EventDisplayRule.of(status, startAt, endAt, now)
}

/**
 * 관심 호스트(M-4)의 호스트별 대표 공연 1개 (#729). 판정은 [V2EventDisplayRule].
 *
 * 1. 진행 중(ONGOING)·예정(UPCOMING) 공개 공연이 있으면 가장 가까운 공연 — 시작 시각 오름차순(진행 중이 앞), 같으면 id 순
 * 2. 없으면 가장 최근에 끝난 공연(PAST) — 종료 시각(없으면 시작 시각) 내림차순, 같으면 id 큰 순
 * 3. 공개 공연이 없으면 null
 *
 * 준비중·삭제 공연은 후보가 아니다 (사용자 앱 공개 공연 = OPEN·CALCULATING·CLOSED, [V2EventBrowseDomainService.PUBLIC_STATUSES]).
 * 쿼리 쪽 상태 필터(ACTIVE = 종료 전 OPEN 존재, [V2EventBrowseQuery.activeCondition])와 같은 기준이다
 */
object V2RepresentativeEventRule {

    fun pick(candidates: List<V2EventSummaryRow>, now: LocalDateTime): V2EventSummaryRow? {
        val public = candidates.filter { it.status in V2EventBrowseDomainService.PUBLIC_STATUSES }
        val (active, past) = public.partition { it.displayStatus(now).isActive() }
        if (active.isNotEmpty()) {
            return active.minWith(compareBy<V2EventSummaryRow> { it.startAt }.thenBy { it.eventId })
        }
        return past.maxWithOrNull(
            compareBy<V2EventSummaryRow, LocalDateTime?>(nullsFirst()) { it.endAt ?: it.startAt }.thenBy { it.eventId },
        )
    }

    private fun V2EventDisplayStatus.isActive(): Boolean =
        this == V2EventDisplayStatus.ONGOING || this == V2EventDisplayStatus.UPCOMING
}
