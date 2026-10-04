package band.gosrock.domain.domains.event.service.v2

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.vo.EventSectionVo
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventContact
import band.gosrock.domain.domains.event.domain.EventPlace
import band.gosrock.domain.domains.event.domain.EventSection
import band.gosrock.domain.domains.event.domain.EventSectionContentFormat
import band.gosrock.domain.domains.event.domain.EventStatus.OPEN
import band.gosrock.domain.domains.event.domain.EventStatus.PREPARING
import band.gosrock.domain.domains.event.exception.CannotChangeHasTicketException
import band.gosrock.domain.domains.event.exception.CannotDeleteNotPreparingEventException
import band.gosrock.domain.domains.event.exception.CannotDisableTicketWithTicketsException
import band.gosrock.domain.domains.event.exception.CannotModifyEndedEventException
import band.gosrock.domain.domains.event.exception.CannotMoveOpenEventStartToPastException
import band.gosrock.domain.domains.event.exception.CannotOpenEventException
import band.gosrock.domain.domains.event.exception.EventCannotEndBeforeStartException
import band.gosrock.domain.domains.event.exception.InvalidEventContactException
import band.gosrock.domain.domains.event.exception.InvalidEventSectionException
import band.gosrock.domain.domains.event.exception.InvalidEventTagException
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.event.service.EventService
import band.gosrock.domain.domains.tag.adaptor.TagAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.springframework.transaction.annotation.Transactional

/**
 * v2 전용 공연 규칙 (DEC-018). v1 코드는 이 서비스를 호출하지 않는다.
 * v1/v2 공통 불변식(상태 전이, end_at 기록, 첫 섹션 ↔ v1 content 동기화)은 [Event] 에 있다.
 */
@DomainService
@Transactional(readOnly = true)
class V2EventDomainService(
    private val eventRepository: EventRepository,
    private val eventService: EventService,
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val tagAdaptor: TagAdaptor,
) {

    /** 간편 생성 (저장 전). 상태는 PREPARING. runTime(분) 은 종료 - 시작으로 계산해 v1 과 맞춘다 */
    fun newEvent(hostId: Long, name: String, startAt: LocalDateTime, endAt: LocalDateTime, hasTicket: Boolean): Event {
        val start = startAt.truncatedTo(ChronoUnit.MINUTES)
        val end = endAt.truncatedTo(ChronoUnit.MINUTES)
        // end_at 은 생성자에서 start + runTime(= end) 으로 기록된다
        return Event(hostId = hostId, name = name, startAt = start, runTime = runTimeMinutesOf(start, end)).also {
            it.changeHasTicket(hasTicket)
        }
    }

    /** 준비중 / 등록된(OPEN) 공연만 수정 가능. 정산중·지난공연은 수정 불가 (삭제 공연은 @Where 로 조회되지 않음) */
    fun validateEditable(event: Event) {
        if (event.status != PREPARING && event.status != OPEN) throw CannotModifyEndedEventException.EXCEPTION
    }

    /**
     * 기본 정보 부분 수정. null 인 항목은 변경하지 않는다. 등록(OPEN) 후에도 수정 가능 (DEC-007)
     * - hasTicket 은 준비중일 때만 바꿀 수 있다 (같은 값은 허용)
     * - startAt / endAt 중 하나만 오면 나머지는 기존 값으로 검증하고, runTime(분) 을 다시 계산해 v1 과 맞춘다
     * - posterImageKey 가 빈 문자열이면 포스터를 비운다
     */
    fun updateBasic(
        event: Event,
        name: String? = null,
        startAt: LocalDateTime? = null,
        endAt: LocalDateTime? = null,
        hasTicket: Boolean? = null,
        posterImageKey: String? = null,
        place: EventPlace? = null,
        hasValidTicket: Boolean = false,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        validateEditable(event)
        if (hasTicket != null && hasTicket != event.hasTicket) {
            if (event.status != PREPARING) throw CannotChangeHasTicketException.EXCEPTION
            // 유효 티켓이 있는데 '티켓 없음' 으로 바꾸면 티켓이 판매되는 무티켓 공연이 된다
            if (!hasTicket && hasValidTicket) throw CannotDisableTicketWithTicketsException.EXCEPTION
            event.changeHasTicket(hasTicket)
        }
        if (name != null || startAt != null || endAt != null) {
            val newStartAt = startAt?.truncatedTo(ChronoUnit.MINUTES)
            // 등록된 공연은 시작 시각을 과거로 옮길 수 없다 (같은 값은 허용 — 시작 후 다른 필드만 수정하는 경우)
            if (event.status == OPEN && newStartAt != null && newStartAt != event.getStartAt() && !newStartAt.isAfter(now)) {
                throw CannotMoveOpenEventStartToPastException.EXCEPTION
            }
            applySchedule(
                event = event,
                name = name ?: event.getEventName(),
                startAt = newStartAt ?: event.getStartAt(),
                endAt = endAt?.truncatedTo(ChronoUnit.MINUTES) ?: event.getEndAt(),
            )
        }
        if (posterImageKey != null) event.changePosterImage(posterImageKey.ifBlank { null })
        if (place != null) event.changePlace(place)
    }

    private fun applySchedule(event: Event, name: String?, startAt: LocalDateTime?, endAt: LocalDateTime?) {
        val runTime = if (startAt != null && endAt != null) runTimeMinutesOf(startAt, endAt) else event.eventBasic?.runTime
        event.changeSchedule(name = name, startAt = startAt, runTime = runTime)
    }

    /** 문의처 전체 교체 (0~[MAX_CONTACT_COUNT]개). 체크리스트 기본 정보는 1개 이상이어야 충족 */
    fun replaceContacts(event: Event, newContacts: List<EventContact>) {
        validateEditable(event)
        if (newContacts.size > MAX_CONTACT_COUNT) throw InvalidEventContactException.EXCEPTION
        newContacts.forEach {
            if (it.value.isBlank() || it.value.length > EventContact.VALUE_MAX_LENGTH) throw InvalidEventContactException.EXCEPTION
        }
        event.replaceContacts(newContacts)
    }

    /** 태그 전체 교체 (중복 id 는 하나로, 최대 [MAX_TAG_COUNT]개). 존재하지 않는 태그 id 가 하나라도 있으면 400 */
    fun replaceTags(event: Event, tagIds: List<Long>) {
        val distinctIds = tagIds.distinct()
        if (tagAdaptor.findAllByIdIn(distinctIds).size != distinctIds.size) throw InvalidEventTagException.EXCEPTION
        validateEditable(event)
        if (distinctIds.size > MAX_TAG_COUNT) throw InvalidEventTagException.EXCEPTION
        event.replaceTagIds(distinctIds)
    }

    /** 섹션 전체 교체 (1~[MAX_SECTION_COUNT]개, 순서는 들어온 순서). 제목 앞뒤 공백은 제거한다. 첫 섹션 본문은 v1 content 에도 기록 */
    fun replaceSections(event: Event, newSections: List<EventSection>) {
        validateEditable(event)
        if (newSections.isEmpty() || newSections.size > MAX_SECTION_COUNT) throw InvalidEventSectionException.EXCEPTION
        newSections.forEach {
            it.title = it.title.trim()
            if (it.title.isEmpty() || it.title.length > EventSection.TITLE_MAX_LENGTH) throw InvalidEventSectionException.EXCEPTION
            if ((it.content?.length ?: 0) > EventSection.CONTENT_MAX_LENGTH) throw InvalidEventSectionException.EXCEPTION
        }
        event.replaceSections(newSections)
    }

    /** 섹션 표시. 섹션이 없는 기존 공연은 v1 content(비어 있지 않으면)를 '공연 소개' 섹션으로 대체 */
    fun displaySections(event: Event): List<EventSectionVo> {
        if (event.sections.isNotEmpty()) {
            return event.sections.map {
                EventSectionVo(sectionId = it.id, title = it.title, content = it.content, contentFormat = it.contentFormat, sortOrder = it.sortOrder)
            }
        }
        val content = event.eventDetail?.content?.takeIf { it.isNotBlank() } ?: return emptyList()
        return listOf(
            EventSectionVo(sectionId = null, title = EventSection.INTRO_TITLE, content = content, contentFormat = EventSectionContentFormat.MARKDOWN, sortOrder = 0),
        )
    }

    /** 체크리스트. 티켓은 유효(삭제 안 된) 티켓만 센다 */
    fun checklist(event: Event, now: LocalDateTime = LocalDateTime.now()): EventChecklist =
        checklist(event, hasValidTicket = ticketItemAdaptor.existsValidByEventId(event.id!!), now = now)

    /** @param hasValidTicket 유효(삭제 안 된) 티켓 존재 여부 */
    fun checklist(event: Event, hasValidTicket: Boolean, now: LocalDateTime): EventChecklist {
        val basic = hasBasic(event)
        val detail = hasDetail(event)
        val filled = basic && detail && (!event.hasTicket || hasValidTicket)
        val startAt = event.getStartAt()
        return EventChecklist(
            basic = basic,
            detail = detail,
            ticket = hasValidTicket,
            ticketRequired = event.hasTicket,
            canOpen = filled && event.status == PREPARING && startAt != null && startAt.isAfter(now),
        )
    }

    /** 기본 정보 충족: 포스터·이름·일정(시작/종료)·장소(이름/주소/좌표)·문의처 1개 이상 (포스터는 v1 공개 목록/v1 open 과 같은 조건) */
    private fun hasBasic(event: Event): Boolean =
        event.eventDetail?.posterImage?.imageKey != null &&
            !event.getEventName().isNullOrBlank() && event.getStartAt() != null && event.getEndAt() != null &&
            event.hasEventPlace() && event.contacts.isNotEmpty()

    /** 상세 정보 충족: 본문이 비어 있지 않은 섹션 1개 이상 (섹션이 없는 기존 공연은 v1 content) */
    private fun hasDetail(event: Event): Boolean = displaySections(event).any { !it.content.isNullOrBlank() }

    /**
     * 등록(OPEN). 준비중이면 체크리스트(hasTicket=false 면 티켓 면제)를 먼저 확인하고,
     * 상태 전이·시작 시각 검증은 v1 과 같은 [Event.open] 에 맡긴다. v1 [EventService.openEvent] 는 바꾸지 않는다
     */
    fun openEvent(event: Event): Event {
        if (event.isPreparing() && !checklist(event).isFilled()) throw CannotOpenEventException.EXCEPTION
        event.open()
        return eventRepository.save(event)
    }

    /** 삭제: 준비중 공연만. 발급 티켓 확인 등 나머지는 v1 [EventService.deleteEventSoft] 규칙 그대로 */
    fun deleteEventSoft(event: Event): Event {
        if (!event.isPreparing()) throw CannotDeleteNotPreparingEventException.EXCEPTION
        return eventService.deleteEventSoft(event)
    }

    companion object {
        const val MAX_CONTACT_COUNT = 10
        const val MAX_SECTION_COUNT = 10
        const val MAX_TAG_COUNT = 10

        /** 종료가 시작보다 늦어야 한다 */
        private fun runTimeMinutesOf(startAt: LocalDateTime, endAt: LocalDateTime): Long {
            if (!endAt.isAfter(startAt)) throw EventCannotEndBeforeStartException.EXCEPTION
            return Duration.between(startAt, endAt).toMinutes()
        }
    }
}
