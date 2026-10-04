package band.gosrock.domain.domains.event

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventBasic
import band.gosrock.domain.domains.event.domain.EventContact
import band.gosrock.domain.domains.event.domain.EventDetail
import band.gosrock.domain.domains.event.domain.EventPlace
import band.gosrock.domain.domains.event.domain.EventSection
import band.gosrock.domain.domains.event.domain.EventSectionContentFormat
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.exception.CannotChangeHasTicketException
import band.gosrock.domain.domains.event.exception.CannotDisableTicketWithTicketsException
import band.gosrock.domain.domains.event.exception.CannotMoveOpenEventStartToPastException
import band.gosrock.domain.domains.event.exception.CannotModifyEndedEventException
import band.gosrock.domain.domains.event.exception.EventCannotEndBeforeStartException
import band.gosrock.domain.domains.event.exception.InvalidEventContactException
import band.gosrock.domain.domains.event.exception.InvalidEventSectionException
import band.gosrock.domain.domains.event.exception.InvalidEventTagException
import band.gosrock.domain.domains.host.domain.HostContactType
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.util.ReflectionTestUtils

/** v2 공연 준비 도메인 규칙 (#705): 시간 계산, 섹션 동기화, 체크리스트 판정 */
class EventV2PrepTest {

    private val start: LocalDateTime = LocalDateTime.of(2030, 5, 11, 18, 0)
    private val end: LocalDateTime = LocalDateTime.of(2030, 5, 11, 21, 20)
    private val now: LocalDateTime = LocalDateTime.of(2030, 1, 1, 0, 0)

    private fun v2Event(hasTicket: Boolean = true): Event =
        Event.createV2(hostId = 1L, name = "정기공연", startAt = start, endAt = end, hasTicket = hasTicket)

    private fun Event.withStatus(status: EventStatus): Event = also { ReflectionTestUtils.setField(it, "status", status) }

    private fun place() = EventPlace(latitude = 37.5, longitude = 126.9, placeName = "롤링홀", placeAddress = "서울 마포구")

    private fun contact(value: String = "@gosrock") = EventContact(type = HostContactType.INSTAGRAM, value = value)

    @Nested
    inner class Schedule {

        @Test
        fun `v2 생성은 runTime 을 종료-시작 분으로 저장하고 v1 endAt 계산과 같다`() {
            val event = v2Event()
            assertEquals(200L, event.eventBasic!!.runTime)
            assertEquals(end, event.storedEndAt)
            assertEquals(end, event.eventBasic!!.endAt())
            assertEquals(end, event.getEndAt())
            assertEquals(EventStatus.PREPARING, event.status)
        }

        @Test
        fun `초 단위는 분으로 잘라 저장한다`() {
            val event = Event.createV2(1L, "공연", start.plusSeconds(30), end.plusSeconds(59), true)
            assertEquals(start, event.getStartAt())
            assertEquals(end, event.getEndAt())
            assertEquals(200L, event.eventBasic!!.runTime)
        }

        @Test
        fun `종료가 시작과 같거나 빠르면 예외`() {
            assertThrows<EventCannotEndBeforeStartException> { Event.createV2(1L, "공연", start, start, true) }
            assertThrows<EventCannotEndBeforeStartException> { Event.createV2(1L, "공연", start, start.minusMinutes(1), true) }
        }

        @Test
        fun `v1 생성 공연도 end_at 이 startAt + runTime 으로 채워진다`() {
            val event = Event(hostId = 1L, name = "v1", startAt = start, runTime = 90L)
            assertEquals(start.plusMinutes(90), event.storedEndAt)
        }

        @Test
        fun `end_at 컬럼이 stale 이어도 기준값은 startAt + runTime 이다 (v1 이 살아있는 동안 end_at 은 쓰기 전용)`() {
            val event = v2Event()
            ReflectionTestUtils.setField(event, "storedEndAt", end.plusHours(5))
            assertEquals(end, event.getEndAt())
            assertEquals(event.eventBasic!!.endAt(), event.getEndAt())
            // 다음 저장에서 end_at 도 기준값으로 다시 기록된다
            event.updateBasicV2(name = "이름만")
            assertEquals(end, event.storedEndAt)
        }

        @Test
        fun `end_at 이 없는 기존 공연은 startAt + runTime 으로 대체`() {
            val event = Event(hostId = 1L, name = "레거시", startAt = start, runTime = 60L)
            ReflectionTestUtils.setField(event, "storedEndAt", null)
            assertEquals(start.plusMinutes(60), event.getEndAt())
        }

        @Test
        fun `v1 PATCH basic 으로 runTime 이 바뀌면 end_at 도 바뀐다`() {
            val event = v2Event()
            event.updateEventBasic(EventBasic(name = "v1수정", startAt = start, runTime = 30L))
            assertEquals(start.plusMinutes(30), event.storedEndAt)
            assertEquals(start.plusMinutes(30), event.getEndAt())
        }

        @Test
        fun `어드민 수정으로 runTime 만 바뀌어도 end_at 이 갱신된다`() {
            val event = v2Event()
            event.adminUpdate(name = null, startAt = null, runTime = 45L, content = null, placeName = null, placeAddress = null)
            assertEquals(start.plusMinutes(45), event.getEndAt())
        }

        @Test
        fun `v2 수정에서 종료만 바꾸면 시작은 유지되고 runTime 이 다시 계산된다`() {
            val event = v2Event()
            event.updateBasicV2(endAt = start.plusMinutes(150))
            assertEquals(start, event.getStartAt())
            assertEquals(150L, event.eventBasic!!.runTime)
            assertEquals(start.plusMinutes(150), event.getEndAt())
        }

        @Test
        fun `v2 수정에서 시작만 바꾸면 기존 종료로 검증한다`() {
            val event = v2Event()
            assertThrows<EventCannotEndBeforeStartException> { event.updateBasicV2(startAt = end) }
            event.updateBasicV2(startAt = start.plusHours(1))
            assertEquals(140L, event.eventBasic!!.runTime)
            assertEquals(end, event.getEndAt())
        }

        @Test
        fun `이름만 바꾸면 일정은 그대로`() {
            val event = v2Event()
            event.updateBasicV2(name = "새 이름")
            assertEquals("새 이름", event.getEventName())
            assertEquals(start, event.getStartAt())
            assertEquals(end, event.getEndAt())
            assertEquals(200L, event.eventBasic!!.runTime)
        }
    }

    @Nested
    inner class Basic {

        @Test
        fun `OPEN 공연은 시작 시각을 현재 이전으로 옮길 수 없고 같은 값-미래는 허용, 준비중은 검증 안 함`() {
            val event = v2Event().withStatus(EventStatus.OPEN)
            assertThrows<CannotMoveOpenEventStartToPastException> { event.updateBasicV2(startAt = now.minusMinutes(1), now = now) }
            assertThrows<CannotMoveOpenEventStartToPastException> { event.updateBasicV2(startAt = now, now = now) }
            // 시작이 지난 뒤에도 같은 startAt 을 함께 보내는 수정은 허용
            event.updateBasicV2(startAt = start, name = "같은 시작", now = start.plusMinutes(10))
            event.updateBasicV2(startAt = start.plusDays(1), endAt = end.plusDays(1), now = now)
            assertEquals(start.plusDays(1), event.getStartAt())

            val preparing = v2Event()
            preparing.updateBasicV2(startAt = now.minusDays(1), now = now)
            assertEquals(now.minusDays(1), preparing.getStartAt())
        }

        @Test
        fun `유효 티켓이 있으면 티켓 없음으로 바꿀 수 없다`() {
            val event = v2Event()
            assertThrows<CannotDisableTicketWithTicketsException> { event.updateBasicV2(hasTicket = false, hasValidTicket = true) }
            assertTrue(event.hasTicket)
            event.updateBasicV2(hasTicket = false, hasValidTicket = false)
            assertFalse(event.hasTicket)
        }

        @Test
        fun `OPEN 공연도 v2 기본 정보는 수정되지만 v1 updateEventBasic 은 여전히 막힌다`() {
            val event = v2Event().withStatus(EventStatus.OPEN)
            event.updateBasicV2(name = "오픈 후 수정", place = place())
            assertEquals("오픈 후 수정", event.getEventName())
            assertThrows<band.gosrock.domain.domains.event.exception.CannotModifyOpenEventException> {
                event.updateEventBasic(EventBasic(name = "v1", startAt = start, runTime = 10L))
            }
        }

        @Test
        fun `hasTicket 은 준비중에서만 바꿀 수 있고 같은 값은 허용`() {
            val event = v2Event()
            event.updateBasicV2(hasTicket = false)
            assertFalse(event.hasTicket)

            event.withStatus(EventStatus.OPEN)
            event.updateBasicV2(hasTicket = false)
            assertThrows<CannotChangeHasTicketException> { event.updateBasicV2(hasTicket = true) }
        }

        @Test
        fun `정산중, 지난공연은 수정 불가`() {
            listOf(EventStatus.CALCULATING, EventStatus.CLOSED).forEach { status ->
                val event = v2Event().withStatus(status)
                assertThrows<CannotModifyEndedEventException> { event.updateBasicV2(name = "x") }
                assertThrows<CannotModifyEndedEventException> { event.replaceSectionsV2(listOf(EventSection("공연 소개", "a"))) }
                assertThrows<CannotModifyEndedEventException> { event.replaceContactsV2(listOf(contact())) }
                assertThrows<CannotModifyEndedEventException> { event.replaceTagIdsV2(listOf(1L)) }
            }
        }

        @Test
        fun `포스터 key 빈 문자열이면 비우고 content 는 유지`() {
            val event = v2Event()
            event.updateEventDetail(EventDetail(posterImageKey = "event/1/a.png", content = "본문"))
            event.updateBasicV2(posterImageKey = "")
            assertNull(event.eventDetail!!.posterImage)
            assertEquals("본문", event.eventDetail!!.content)
        }

        @Test
        fun `문의처는 0~10개, 빈 값-200자 초과는 예외`() {
            val event = v2Event()
            event.replaceContactsV2(listOf(contact("a"), contact("b")))
            assertEquals(listOf(0, 1), event.contacts.map { it.sortOrder })
            event.replaceContactsV2(emptyList())
            assertTrue(event.contacts.isEmpty())
            assertThrows<InvalidEventContactException> { event.replaceContactsV2((1..11).map { contact("$it") }) }
            assertThrows<InvalidEventContactException> { event.replaceContactsV2(listOf(contact(" "))) }
            assertThrows<InvalidEventContactException> { event.replaceContactsV2(listOf(contact("a".repeat(201)))) }
        }

        @Test
        fun `태그는 차이만 반영하고 중복은 하나로, 10개 초과는 예외`() {
            val event = v2Event()
            event.replaceTagIdsV2(listOf(1L, 2L, 2L, 3L))
            val kept = event.tags.first { it.tagId == 2L }
            event.replaceTagIdsV2(listOf(2L, 4L))
            assertEquals(setOf(2L, 4L), event.getTagIds().toSet())
            assertTrue(event.tags.any { it === kept }, "유지된 태그는 같은 엔티티여야 함 (unique 충돌 방지)")
            assertThrows<InvalidEventTagException> { event.replaceTagIdsV2((1L..11L).toList()) }
        }
    }

    @Nested
    inner class Sections {

        @Test
        fun `섹션 저장 시 제목과 무관하게 첫 섹션 본문이 v1 content 로 기록되고 순서가 다시 매겨진다`() {
            val event = v2Event()
            event.updateEventDetail(EventDetail(posterImageKey = "p.png", content = "v1 본문"))
            event.replaceSectionsV2(
                listOf(EventSection(" 예매안내 ", "<p>예매</p>"), EventSection("공연 소개", "<p>소개</p>"), EventSection("추가", null)),
            )
            assertEquals(listOf("예매안내", "공연 소개", "추가"), event.sections.map { it.title })
            assertEquals(listOf(0, 1, 2), event.sections.map { it.sortOrder })
            assertEquals("예매안내", event.findIntroSection()!!.title)
            assertEquals("<p>예매</p>", event.eventDetail!!.content)
            assertEquals("p.png", event.eventDetail!!.posterImage!!.imageKey)
            assertTrue(event.sections.all { it.contentFormat == EventSectionContentFormat.HTML })
        }

        @Test
        fun `v1 content 가 바뀌면 첫 섹션이 갱신되어 MARKDOWN 이 되고 다른 섹션은 그대로`() {
            val event = v2Event()
            event.replaceSectionsV2(listOf(EventSection("소개", "old"), EventSection("유의사항", "주의")))
            event.updateEventDetail(EventDetail(posterImageKey = "p.png", content = "v1 new"))
            assertEquals(listOf("v1 new", "주의"), event.displaySectionsV2().map { it.content })
            assertEquals(
                listOf(EventSectionContentFormat.MARKDOWN, EventSectionContentFormat.HTML),
                event.displaySectionsV2().map { it.contentFormat },
            )

            event.adminUpdate(name = null, startAt = null, runTime = null, content = "admin new", placeName = null, placeAddress = null)
            assertEquals("admin new", event.findIntroSection()!!.content)
        }

        @Test
        fun `섹션이 없는 공연은 v1 content 로 대체 표시하고, content 도 없으면 빈 목록`() {
            val event = v2Event()
            assertTrue(event.displaySectionsV2().isEmpty())
            event.updateEventDetail(EventDetail(posterImageKey = null, content = "기존 본문"))
            // 섹션이 없으면 v1 변경은 섹션을 만들지 않는다
            assertTrue(event.sections.isEmpty())
            val display = event.displaySectionsV2().single()
            assertNull(display.sectionId)
            assertEquals(EventSection.INTRO_TITLE, display.title)
            assertEquals("기존 본문", display.content)
            assertEquals(EventSectionContentFormat.MARKDOWN, display.contentFormat)
        }

        @Test
        fun `섹션 0개, 11개, 제목 공백-21자, 본문 초과는 예외`() {
            val event = v2Event()
            assertThrows<InvalidEventSectionException> { event.replaceSectionsV2(emptyList()) }
            assertThrows<InvalidEventSectionException> { event.replaceSectionsV2((1..11).map { EventSection("s$it", "") }) }
            assertThrows<InvalidEventSectionException> { event.replaceSectionsV2(listOf(EventSection("  ", "a"))) }
            assertThrows<InvalidEventSectionException> { event.replaceSectionsV2(listOf(EventSection("가".repeat(21), "a"))) }
            assertThrows<InvalidEventSectionException> {
                event.replaceSectionsV2(listOf(EventSection("공연 소개", "a".repeat(EventSection.CONTENT_MAX_LENGTH + 1))))
            }
            event.replaceSectionsV2((1..10).map { EventSection("가".repeat(20), "") })
            assertEquals(10, event.sections.size)
        }
    }

    @Nested
    inner class Checklist {

        private fun filledEvent(hasTicket: Boolean = true): Event = v2Event(hasTicket).also {
            it.updateBasicV2(place = place(), posterImageKey = "event/1/poster.png")
            it.replaceContactsV2(listOf(contact()))
            it.replaceSectionsV2(listOf(EventSection("공연 소개", "소개")))
        }

        @Test
        fun `기본-상세-티켓 충족 + 시작 전 + 준비중이면 canOpen`() {
            val checklist = filledEvent().checklistV2(hasValidTicket = true, now = now)
            assertTrue(checklist.basic && checklist.detail && checklist.ticket && checklist.ticketRequired)
            assertTrue(checklist.canOpen)
        }

        @Test
        fun `기본 정보는 포스터-장소-문의처가 모두 있어야 한다`() {
            val noContact = v2Event().also { it.updateBasicV2(place = place(), posterImageKey = "p.png") }
            assertFalse(noContact.checklistV2(true, now).basic)
            val noPlace = v2Event().also { it.replaceContactsV2(listOf(contact())); it.updateBasicV2(posterImageKey = "p.png") }
            assertFalse(noPlace.checklistV2(true, now).basic)
            val noPoster = filledEvent().also { it.updateBasicV2(posterImageKey = "") }
            assertFalse(noPoster.checklistV2(true, now).basic)
            noPoster.updateBasicV2(posterImageKey = "p.png")
            assertTrue(noPoster.checklistV2(true, now).basic)
        }

        @Test
        fun `상세 정보는 본문 있는 섹션 1개 이상, 섹션이 없으면 v1 content`() {
            val event = v2Event()
            event.replaceSectionsV2(listOf(EventSection("공연 소개", " "), EventSection("유의사항", null)))
            assertFalse(event.checklistV2(true, now).detail)
            event.replaceSectionsV2(listOf(EventSection("유의사항", "주의")))
            assertTrue(event.checklistV2(true, now).detail)

            val legacy = v2Event()
            assertFalse(legacy.checklistV2(true, now).detail)
            legacy.updateEventDetail(EventDetail(posterImageKey = null, content = "v1 content"))
            assertTrue(legacy.checklistV2(true, now).detail)
        }

        @Test
        fun `티켓 없음 공연은 티켓 항목 면제`() {
            val checklist = filledEvent(hasTicket = false).checklistV2(hasValidTicket = false, now = now)
            assertFalse(checklist.ticket)
            assertFalse(checklist.ticketRequired)
            assertTrue(checklist.canOpen)
            assertTrue(checklist.isFilled())
        }

        @Test
        fun `티켓 있음 공연은 유효 티켓이 없으면 등록 불가`() {
            val checklist = filledEvent().checklistV2(hasValidTicket = false, now = now)
            assertTrue(checklist.ticketRequired)
            assertFalse(checklist.canOpen)
            assertFalse(checklist.isFilled())
        }

        @Test
        fun `시작이 지났거나 준비중이 아니면 canOpen false`() {
            assertFalse(filledEvent().checklistV2(true, now = start.plusMinutes(1)).canOpen)
            val open = filledEvent().withStatus(EventStatus.OPEN)
            val checklist = open.checklistV2(true, now)
            assertTrue(checklist.isFilled())
            assertFalse(checklist.canOpen)
        }
    }
}
