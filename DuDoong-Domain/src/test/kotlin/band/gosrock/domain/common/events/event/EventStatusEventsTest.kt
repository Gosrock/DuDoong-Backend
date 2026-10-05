package band.gosrock.domain.common.events.event

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils

/** 호스트 공연 상태 변경·삭제 이벤트가 선물 연쇄(#734)에 필요한 값(바뀐 상태, 공연 id)을 싣는지 */
class EventStatusEventsTest {

    private fun event(status: EventStatus) =
        Event(hostId = 1L, name = "공연", startAt = LocalDateTime.now().plusDays(1), runTime = 120L).also {
            ReflectionTestUtils.setField(it, "id", 10L)
            ReflectionTestUtils.setField(it, "status", status)
        }

    @Test
    fun `상태 변경 이벤트는 바뀐 상태, 삭제 이벤트는 공연 id 를 싣는다`() {
        assertEquals(EventStatus.PREPARING, EventStatusChangeEvent.of(event(EventStatus.PREPARING)).status)
        assertEquals(EventStatus.CALCULATING, EventStatusChangeEvent.of(event(EventStatus.CALCULATING)).status)
        assertEquals(10L, EventDeletionEvent.of(event(EventStatus.DELETED)).eventId)
    }
}
