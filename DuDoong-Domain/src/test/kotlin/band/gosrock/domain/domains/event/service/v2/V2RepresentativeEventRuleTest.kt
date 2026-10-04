package band.gosrock.domain.domains.event.service.v2

import band.gosrock.domain.domains.event.domain.EventStatus
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** 관심 호스트(M-4) 대표 공연 선택 (#729) */
class V2RepresentativeEventRuleTest {

    private val now = LocalDateTime.of(2026, 10, 5, 12, 0)

    private fun row(id: Long, status: EventStatus, startAt: LocalDateTime?, runTime: Long? = 120) =
        V2EventSummaryRow(eventId = id, hostId = 1, name = "공연$id", posterImageKey = null, status = status, startAt = startAt, runTime = runTime)

    private fun pick(vararg rows: V2EventSummaryRow) = V2RepresentativeEventRule.pick(rows.toList(), now)?.eventId

    @Test
    fun `진행 중 공연이 있으면 예정 공연보다 진행 중 공연 (시작이 더 이르다)`() {
        val ongoing = row(1, EventStatus.OPEN, now.minusHours(1))
        val upcoming = row(2, EventStatus.OPEN, now.plusDays(1))
        val closed = row(3, EventStatus.CLOSED, now.minusDays(1))
        assertEquals(1, pick(upcoming, closed, ongoing))
    }

    @Test
    fun `예정 공연만 있으면 가장 가까운 공연, 같은 시작이면 id 작은 것`() {
        assertEquals(5, pick(row(4, EventStatus.OPEN, now.plusDays(3)), row(5, EventStatus.OPEN, now.plusDays(1)), row(6, EventStatus.OPEN, now.plusDays(2))))
        assertEquals(7, pick(row(8, EventStatus.OPEN, now.plusDays(1)), row(7, EventStatus.OPEN, now.plusDays(1))))
    }

    @Test
    fun `진행 중·예정이 있으면 더 최근에 끝난 공연이 있어도 진행 중·예정`() {
        assertEquals(2, pick(row(1, EventStatus.OPEN, now.minusHours(3)), row(2, EventStatus.OPEN, now.plusDays(30)), row(3, EventStatus.CALCULATING, now.minusHours(4))))
    }

    @Test
    fun `끝난 공연만 있으면 종료 시각이 가장 최근인 공연 — OPEN 종료·정산중·종료 모두 대상`() {
        // 종료 시각: 1 = now-3h+2h = now-1h (종료 배치 전 OPEN), 2 = now-2d+2h, 3 = now-1d+2h
        val endedOpen = row(1, EventStatus.OPEN, now.minusHours(3))
        val closed = row(2, EventStatus.CLOSED, now.minusDays(2))
        val calculating = row(3, EventStatus.CALCULATING, now.minusDays(1))
        assertEquals(1, pick(closed, calculating, endedOpen))
        assertEquals(3, pick(closed, calculating))
    }

    @Test
    fun `끝난 공연 - 시작이 늦어도 종료가 더 이르면 뒤로 (러닝타임 반영), 종료가 같으면 id 큰 것`() {
        // 4: 시작 now-10d, 러닝타임 3일 → 종료 now-7d / 5: 시작 now-9d, 2시간 → 종료 now-9d+2h
        assertEquals(4, pick(row(4, EventStatus.CLOSED, now.minusDays(10), runTime = 3 * 24 * 60L), row(5, EventStatus.CLOSED, now.minusDays(9))))
        assertEquals(7, pick(row(6, EventStatus.CLOSED, now.minusDays(1)), row(7, EventStatus.CLOSED, now.minusDays(1))))
    }

    @Test
    fun `러닝타임 없음 - 종료 = 시작으로 본다 (표시 상태 규칙과 같음)`() {
        // 시작 시각이 지났고 러닝타임이 없으면 PAST
        assertEquals(1, pick(row(1, EventStatus.OPEN, now.minusMinutes(1), runTime = null), row(2, EventStatus.CLOSED, now.minusDays(1))))
        assertEquals(3, pick(row(3, EventStatus.OPEN, now.plusMinutes(1), runTime = null), row(4, EventStatus.CLOSED, now.minusDays(1))))
    }

    @Test
    fun `공연이 없으면 null`() {
        assertNull(pick())
    }

    @Test
    fun `준비중·삭제 공연은 후보에서 제외 — 그것뿐이면 null`() {
        val preparing = row(1, EventStatus.PREPARING, now.plusDays(1))
        val deleted = row(2, EventStatus.DELETED, now.plusDays(1))
        assertNull(pick(preparing, deleted))
        assertEquals(3, pick(preparing, deleted, row(3, EventStatus.CLOSED, now.minusDays(5))))
    }

    @Test
    fun `시작 시각이 없는 OPEN 은 PAST 취급, 다른 끝난 공연보다 뒤`() {
        assertEquals(2, pick(row(1, EventStatus.OPEN, null), row(2, EventStatus.CLOSED, now.minusDays(100))))
        assertEquals(1, pick(row(1, EventStatus.OPEN, null)))
    }
}
