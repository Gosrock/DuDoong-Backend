package band.gosrock.api.v2.host

import band.gosrock.api.v2.host.dto.response.V2EventDisplayStatus
import band.gosrock.domain.domains.event.domain.EventStatus
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class V2EventDisplayStatusTest {

    private val now = LocalDateTime.of(2026, 10, 4, 12, 0)

    @Test
    fun `PREPARING 은 PREPARING`() {
        assertEquals(V2EventDisplayStatus.PREPARING, V2EventDisplayStatus.of(EventStatus.PREPARING, null, now))
    }

    @Test
    fun `OPEN 은 시작 전이면 UPCOMING, 시작 시각 이후면 PAST`() {
        assertEquals(V2EventDisplayStatus.UPCOMING, V2EventDisplayStatus.of(EventStatus.OPEN, now.plusMinutes(1), now))
        assertEquals(V2EventDisplayStatus.PAST, V2EventDisplayStatus.of(EventStatus.OPEN, now, now))
        assertEquals(V2EventDisplayStatus.PAST, V2EventDisplayStatus.of(EventStatus.OPEN, now.minusDays(1), now))
    }

    @Test
    fun `CALCULATING, CLOSED 는 시작 시각과 무관하게 PAST`() {
        assertEquals(V2EventDisplayStatus.PAST, V2EventDisplayStatus.of(EventStatus.CALCULATING, now.plusDays(1), now))
        assertEquals(V2EventDisplayStatus.PAST, V2EventDisplayStatus.of(EventStatus.CLOSED, now.plusDays(1), now))
    }
}
