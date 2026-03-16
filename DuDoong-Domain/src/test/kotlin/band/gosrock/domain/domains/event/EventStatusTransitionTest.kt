package band.gosrock.domain.domains.event

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventBasic
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.exception.AlreadyCalculatingStatusException
import band.gosrock.domain.domains.event.exception.AlreadyCloseStatusException
import band.gosrock.domain.domains.event.exception.AlreadyDeletedStatusException
import band.gosrock.domain.domains.event.exception.AlreadyOpenStatusException
import band.gosrock.domain.domains.event.exception.AlreadyPreparingStatusException
import band.gosrock.domain.domains.event.exception.CannotDeleteByOpenEventException
import band.gosrock.domain.domains.event.exception.CannotModifyOpenEventException
import band.gosrock.domain.domains.event.exception.EventNotOpenException
import band.gosrock.domain.domains.event.exception.EventOpenTimeExpiredException
import band.gosrock.domain.domains.event.exception.InvalidEventStatusTransitionException
import band.gosrock.domain.domains.event.exception.EventTicketingTimeIsPassedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

class EventStatusTransitionTest {

    private lateinit var event: Event

    @BeforeEach
    fun setUp() {
        event = Event.builder().build()
    }

    // ---- hasEventBasic ----

    @Test
    fun `name과 startAt과 runTime이 모두 있으면 hasEventBasic이 true다`() {
        val basic = EventBasic.builder()
            .name("공연명")
            .startAt(LocalDateTime.now().plusDays(1))
            .runTime(90L)
            .build()
        event.updateEventBasic(basic)
        assertTrue(event.hasEventBasic())
    }

    @Test
    fun `eventBasic이 null이면 hasEventBasic이 false다`() {
        event.updateEventBasic(null)
        assertFalse(event.hasEventBasic())
    }

    @Test
    fun `name이 없으면 hasEventBasic이 false다`() {
        val basic = EventBasic.builder()
            .startAt(LocalDateTime.now().plusDays(1))
            .runTime(90L)
            .build()
        event.updateEventBasic(basic)
        assertFalse(event.hasEventBasic())
    }

    // ---- isTimeBeforeStartAt ----

    @Test
    fun `startAt이 미래이면 isTimeBeforeStartAt이 true다`() {
        val basic = EventBasic.builder()
            .name("공연명")
            .startAt(LocalDateTime.now().plusDays(1))
            .runTime(90L)
            .build()
        event.updateEventBasic(basic)
        assertTrue(event.isTimeBeforeStartAt())
    }

    @Test
    fun `startAt이 과거이면 isTimeBeforeStartAt이 false다`() {
        val basic = EventBasic.builder()
            .name("공연명")
            .startAt(LocalDateTime.now().minusMinutes(1))
            .runTime(90L)
            .build()
        event.updateEventBasic(basic)
        assertFalse(event.isTimeBeforeStartAt())
    }

    // ---- validateTicketingTime ----

    @Test
    fun `startAt이 과거이면 validateTicketingTime이 예외를 던진다`() {
        val basic = EventBasic.builder()
            .name("공연명")
            .startAt(LocalDateTime.now().minusMinutes(1))
            .runTime(90L)
            .build()
        event.updateEventBasic(basic)
        assertThrows(EventTicketingTimeIsPassedException::class.java) {
            event.validateTicketingTime()
        }
    }

    @Test
    fun `startAt이 미래이면 validateTicketingTime이 예외를 던지지 않는다`() {
        val basic = EventBasic.builder()
            .name("공연명")
            .startAt(LocalDateTime.now().plusHours(1))
            .runTime(90L)
            .build()
        event.updateEventBasic(basic)
        event.validateTicketingTime() // no exception
    }

    // ---- status: PREPARING (default) ----

    @Test
    fun `신규 이벤트의 기본 상태는 PREPARING이다`() {
        assertEquals(EventStatus.PREPARING, event.status)
    }

    @Test
    fun `이미 PREPARING 상태에서 prepare를 호출하면 AlreadyPreparingStatusException이 발생한다`() {
        assertThrows(AlreadyPreparingStatusException::class.java) {
            event.prepare()
        }
    }

    // ---- status: OPEN ----

    @Test
    fun `미래 startAt이 있는 이벤트는 open 상태로 변경된다`() {
        val basic = EventBasic.builder()
            .name("공연명")
            .startAt(LocalDateTime.now().plusMinutes(10))
            .runTime(90L)
            .build()
        event.updateEventBasic(basic)
        event.open()
        assertEquals(EventStatus.OPEN, event.status)
    }

    @Test
    fun `이미 OPEN 상태에서 open을 호출하면 AlreadyOpenStatusException이 발생한다`() {
        val basic = EventBasic.builder()
            .name("공연명")
            .startAt(LocalDateTime.now().plusMinutes(10))
            .runTime(90L)
            .build()
        event.updateEventBasic(basic)
        event.open()
        assertThrows(AlreadyOpenStatusException::class.java) {
            event.open()
        }
    }

    @Test
    fun `과거 startAt이 있는 이벤트는 open을 호출하면 EventOpenTimeExpiredException이 발생한다`() {
        val basic = EventBasic.builder()
            .name("공연명")
            .startAt(LocalDateTime.now().minusMinutes(1))
            .runTime(90L)
            .build()
        event.updateEventBasic(basic)
        assertThrows(EventOpenTimeExpiredException::class.java) {
            event.open()
        }
    }

    // ---- status: CALCULATING ----

    @Test
    fun `OPEN 이벤트는 CALCULATING으로 변경된다`() {
        ReflectionTestUtils.setField(event, "status", EventStatus.OPEN)
        event.calculate()
        assertEquals(EventStatus.CALCULATING, event.status)
    }

    @Test
    fun `PREPARING에서 CALCULATING으로 직접 전이는 불가하다`() {
        assertThrows(InvalidEventStatusTransitionException::class.java) {
            event.calculate()
        }
    }

    @Test
    fun `이미 CALCULATING 상태에서 calculate를 호출하면 AlreadyCalculatingStatusException이 발생한다`() {
        ReflectionTestUtils.setField(event, "status", EventStatus.OPEN)
        event.calculate()
        assertThrows(AlreadyCalculatingStatusException::class.java) {
            event.calculate()
        }
    }

    // ---- status: CLOSED ----

    @Test
    fun `CALCULATING 이벤트는 CLOSED로 변경된다`() {
        ReflectionTestUtils.setField(event, "status", EventStatus.CALCULATING)
        event.close()
        assertEquals(EventStatus.CLOSED, event.status)
    }

    @Test
    fun `OPEN에서 CLOSED로 직접 전이는 불가하다`() {
        ReflectionTestUtils.setField(event, "status", EventStatus.OPEN)
        assertThrows(InvalidEventStatusTransitionException::class.java) {
            event.close()
        }
    }

    @Test
    fun `이미 CLOSED 상태에서 close를 호출하면 AlreadyCloseStatusException이 발생한다`() {
        ReflectionTestUtils.setField(event, "status", EventStatus.CALCULATING)
        event.close()
        assertThrows(AlreadyCloseStatusException::class.java) {
            event.close()
        }
    }

    // ---- status: DELETED ----

    @Test
    fun `PREPARING 이벤트는 소프트 삭제된다`() {
        event.deleteSoft()
        assertEquals(EventStatus.DELETED, event.status)
    }

    @Test
    fun `OPEN 이벤트는 deleteSoft를 호출하면 CannotDeleteByOpenEventException이 발생한다`() {
        ReflectionTestUtils.setField(event, "status", EventStatus.OPEN)
        assertThrows(CannotDeleteByOpenEventException::class.java) {
            event.deleteSoft()
        }
    }

    @Test
    fun `이미 DELETED 상태에서 deleteSoft를 호출하면 AlreadyDeletedStatusException이 발생한다`() {
        event.deleteSoft()
        assertThrows(AlreadyDeletedStatusException::class.java) {
            event.deleteSoft()
        }
    }

    // ---- validateOpenStatus / validateNotOpenStatus ----

    @Test
    fun `OPEN 상태에서 validateOpenStatus를 호출하면 CannotModifyOpenEventException이 발생한다`() {
        ReflectionTestUtils.setField(event, "status", EventStatus.OPEN)
        assertThrows(CannotModifyOpenEventException::class.java) {
            event.validateOpenStatus()
        }
    }

    @Test
    fun `PREPARING 상태에서 validateNotOpenStatus를 호출하면 EventNotOpenException이 발생한다`() {
        assertThrows(EventNotOpenException::class.java) {
            event.validateNotOpenStatus()
        }
    }

    @Test
    fun `OPEN 상태에서 validateNotOpenStatus는 예외를 던지지 않는다`() {
        ReflectionTestUtils.setField(event, "status", EventStatus.OPEN)
        event.validateNotOpenStatus() // no exception
    }

    // ---- getEventName ----

    @Test
    fun `eventBasic이 있으면 getEventName이 이름을 반환한다`() {
        val basic = EventBasic.builder()
            .name("두둥 공연")
            .startAt(LocalDateTime.now().plusDays(1))
            .runTime(60L)
            .build()
        event.updateEventBasic(basic)
        assertEquals("두둥 공연", event.getEventName())
    }

    @Test
    fun `eventBasic이 null이면 getEventName이 null을 반환한다`() {
        assertNull(event.getEventName())
    }

    // ---- isPreparing / isClosed ----

    @Test
    fun `PREPARING 상태이면 isPreparing이 true다`() {
        assertTrue(event.isPreparing())
    }

    @Test
    fun `CLOSED 상태이면 isClosed가 true다`() {
        ReflectionTestUtils.setField(event, "status", EventStatus.CLOSED)
        assertTrue(event.isClosed())
    }
}
