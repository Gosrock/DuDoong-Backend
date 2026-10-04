package band.gosrock.domain.domains.event.service.v2

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import java.time.LocalDateTime

/** v2 표시용 공연 상태 (호스팅 센터 E-1·E-3·E-8·E-9·H-14 와 사용자 앱 P-1~P-3 공통). 사용자 앱에는 PREPARING 이 나오지 않는다 */
enum class V2EventDisplayStatus {
    PREPARING,
    UPCOMING,
    ONGOING,
    PAST,
}

/**
 * v2 표시 상태 판정 — 한 곳에서만 정한다 (#716 M-3). 쿼리 쪽 조건([V2EventBrowseQuery])도 이 기준과 같다.
 *
 * - PREPARING → PREPARING
 * - OPEN: 시작 전 → UPCOMING, 시작 ~ 종료 전 → ONGOING, 종료 후(종료 배치가 아직 CALCULATING 으로 안 넘긴 경우) → PAST
 *   - 종료 시각 = startAt + runTime(분) ([Event.getEndAt], DEC-019 #6 — 종료 배치·정산과 같은 계산값). runTime 이 없으면 종료 = 시작
 *   - startAt 이 없으면(방어: prod 0건, 2026-10-04) 일정 판단이 안 되므로 PAST — 홈·기본 목록에 띄우지 않는다
 * - CALCULATING / CLOSED / DELETED → PAST
 */
object V2EventDisplayRule {

    fun of(event: Event, now: LocalDateTime): V2EventDisplayStatus = of(event.status, event.getStartAt(), event.getEndAt(), now)

    fun of(status: EventStatus, startAt: LocalDateTime?, endAt: LocalDateTime?, now: LocalDateTime): V2EventDisplayStatus =
        when (status) {
            EventStatus.PREPARING -> V2EventDisplayStatus.PREPARING
            EventStatus.OPEN -> when {
                startAt == null -> V2EventDisplayStatus.PAST
                startAt.isAfter(now) -> V2EventDisplayStatus.UPCOMING
                (endAt ?: startAt).isAfter(now) -> V2EventDisplayStatus.ONGOING
                else -> V2EventDisplayStatus.PAST
            }
            EventStatus.CALCULATING, EventStatus.CLOSED, EventStatus.DELETED -> V2EventDisplayStatus.PAST
        }
}
