package band.gosrock.domain.domains.event.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils

/** v2 상세주소(#740)를 모르는 v1·운영 어드민 장소 수정: 주소가 같으면 유지, 바뀌면 지움 */
class EventPlaceDetailTest {

    private fun place(address: String?, detail: String? = null) =
        EventPlace(latitude = 37.5, longitude = 126.9, placeName = "롤링홀", placeAddress = address, placeDetailAddress = detail)

    private fun eventWith(place: EventPlace?) = Event(hostId = null, name = null).also { ReflectionTestUtils.setField(it, "eventPlace", place) }

    @Test
    fun `keepingDetailOf — 주소가 같으면 이전 상세주소, 다르면 null, 새 값이 있으면 새 값`() {
        assertEquals("B1", place("주소A").keepingDetailOf(place("주소A", "B1")).placeDetailAddress)
        assertNull(place("주소B").keepingDetailOf(place("주소A", "B1")).placeDetailAddress)
        assertNull(place("주소A").keepingDetailOf(null).placeDetailAddress)
        assertEquals("2층", place("주소A", "2층").keepingDetailOf(place("주소A", "B1")).placeDetailAddress)
    }

    @Test
    fun `v1 updateEventPlace — 준비중 공연에서 같은 주소면 유지, 다른 주소면 지움`() {
        val event = eventWith(place("주소A", "B1"))
        event.updateEventPlace(place("주소A"))
        assertEquals("B1", event.eventPlace!!.placeDetailAddress)
        event.updateEventPlace(place("주소B"))
        assertNull(event.eventPlace!!.placeDetailAddress)
    }

    @Test
    fun `운영 어드민 adminUpdate — 이름만 바꾸면 유지, 주소를 바꾸면 지움`() {
        val event = eventWith(place("주소A", "B1"))
        event.adminUpdate(name = null, startAt = null, runTime = null, content = null, placeName = "새 이름", placeAddress = null)
        assertEquals("B1", event.eventPlace!!.placeDetailAddress)
        assertEquals("새 이름", event.eventPlace!!.placeName)
        event.adminUpdate(name = null, startAt = null, runTime = null, content = null, placeName = null, placeAddress = "주소B")
        assertNull(event.eventPlace!!.placeDetailAddress)
    }
}
