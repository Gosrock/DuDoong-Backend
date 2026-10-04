package band.gosrock.domain.domains.event.service.v2

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils

/** v2 표시 상태 판정 (#716 M-3): 호스팅 센터·사용자 앱 공통 */
class V2EventDisplayRuleTest {

    private val now = LocalDateTime.of(2026, 10, 4, 12, 0)

    private fun of(status: EventStatus, startAt: LocalDateTime?, endAt: LocalDateTime?) = V2EventDisplayRule.of(status, startAt, endAt, now)

    @Test
    fun `PREPARING 은 일정과 무관하게 PREPARING`() {
        assertEquals(V2EventDisplayStatus.PREPARING, of(EventStatus.PREPARING, null, null))
        assertEquals(V2EventDisplayStatus.PREPARING, of(EventStatus.PREPARING, now.minusDays(1), now.minusHours(1)))
    }

    @Test
    fun `OPEN - 시작 전 UPCOMING, 시작 시각부터 종료 전까지 ONGOING, 종료 시각부터 PAST`() {
        assertEquals(V2EventDisplayStatus.UPCOMING, of(EventStatus.OPEN, now.plusMinutes(1), now.plusHours(2)))
        assertEquals(V2EventDisplayStatus.ONGOING, of(EventStatus.OPEN, now, now.plusHours(2)))
        assertEquals(V2EventDisplayStatus.ONGOING, of(EventStatus.OPEN, now.minusHours(1), now.plusMinutes(1)))
        assertEquals(V2EventDisplayStatus.PAST, of(EventStatus.OPEN, now.minusHours(2), now))
        assertEquals(V2EventDisplayStatus.PAST, of(EventStatus.OPEN, now.minusDays(1), now.minusHours(22)))
    }

    @Test
    fun `OPEN - 시작 시각이 없으면 PAST, 종료 시각이 없으면 종료 = 시작`() {
        assertEquals(V2EventDisplayStatus.PAST, of(EventStatus.OPEN, null, null))
        assertEquals(V2EventDisplayStatus.UPCOMING, of(EventStatus.OPEN, now.plusMinutes(1), null))
        assertEquals(V2EventDisplayStatus.PAST, of(EventStatus.OPEN, now, null))
    }

    @Test
    fun `CALCULATING CLOSED DELETED 는 일정과 무관하게 PAST`() {
        listOf(EventStatus.CALCULATING, EventStatus.CLOSED, EventStatus.DELETED).forEach {
            assertEquals(V2EventDisplayStatus.PAST, of(it, now.plusDays(1), now.plusDays(1).plusHours(2)))
        }
    }

    @Test
    fun `엔티티 판정은 종료 = startAt + runTime 을 쓴다 (장기 공연 포함)`() {
        // prod 최대 runTime 30,000분(약 20.8일)
        val festival = Event(hostId = 1L, name = "페스티벌", startAt = now.minusDays(20), runTime = 30_000L)
            .also { ReflectionTestUtils.setField(it, "status", EventStatus.OPEN) }
        assertEquals(V2EventDisplayStatus.ONGOING, V2EventDisplayRule.of(festival, now))
        assertEquals(V2EventDisplayStatus.PAST, V2EventDisplayRule.of(festival, now.minusDays(20).plusMinutes(30_000)))
    }
}
