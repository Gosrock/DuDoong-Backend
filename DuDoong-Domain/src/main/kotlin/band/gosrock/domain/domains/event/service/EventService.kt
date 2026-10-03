package band.gosrock.domain.domains.event.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventBasic
import band.gosrock.domain.domains.event.domain.EventDetail
import band.gosrock.domain.domains.event.domain.EventPlace
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.exception.CannotDeleteByIssuedTicketException
import band.gosrock.domain.domains.event.exception.CannotOpenEventException
import band.gosrock.domain.domains.event.exception.UseOtherApiException
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor
import band.gosrock.domain.domains.ticket_item.service.TicketItemService
import java.time.LocalDateTime
import org.springframework.transaction.annotation.Transactional

@DomainService
@Transactional(readOnly = true)
class EventService(
    private val eventRepository: EventRepository,
    private val eventAdaptor: EventAdaptor,
    private val ticketItemService: TicketItemService,
    private val issuedTicketAdaptor: IssuedTicketAdaptor,
) {

    fun createEvent(event: Event): Event = eventRepository.save(event)

    fun updateEventBasic(event: Event, eventBasic: EventBasic, eventPlace: EventPlace): Event {
        event.updateEventBasic(eventBasic)
        event.updateEventPlace(eventPlace)
        return eventRepository.save(event)
    }

    fun updateEventDetail(event: Event, eventDetail: EventDetail): Event {
        event.updateEventDetail(eventDetail)
        return eventRepository.save(event)
    }

    fun updateEventPlace(event: Event, eventPlace: EventPlace): Event {
        event.updateEventPlace(eventPlace)
        return eventRepository.save(event)
    }

    fun validateEventBasicExistence(event: Event) {
        if (!event.hasEventBasic() || !event.hasEventPlace()) throw CannotOpenEventException.EXCEPTION
    }

    fun validateEventDetailExistence(event: Event) {
        if (!event.hasEventDetail()) throw CannotOpenEventException.EXCEPTION
    }

    fun openEvent(event: Event): Event {
        validateEventBasicExistence(event)
        validateEventDetailExistence(event)
        ticketItemService.validateExistenceByEventId(event.id!!)
        event.open()
        return eventRepository.save(event)
    }

    fun updateEventStatus(event: Event, status: EventStatus): Event {
        when (status) {
            EventStatus.CLOSED -> event.close()
            EventStatus.CALCULATING -> event.calculate()
            EventStatus.PREPARING -> event.prepare()
            else -> throw UseOtherApiException.EXCEPTION // open, deleteSoft 는 다른 API 강제
        }
        return eventRepository.save(event)
    }

    fun closeExpiredEventsEndAtBefore(time: LocalDateTime): List<Event> {
        val events = eventAdaptor.queryEventsByEndAtBeforeAndStatusOpen(time)
        events.forEach { event ->
            updateEventStatus(event, EventStatus.CALCULATING)
            updateEventStatus(event, EventStatus.CLOSED)
        }
        eventRepository.saveAll(events)
        return events
    }

    fun deleteEventSoft(event: Event): Event {
        // 발급된 티켓이 있다면 삭제 불가
        if (issuedTicketAdaptor.existsByEventId(event.id!!)) throw CannotDeleteByIssuedTicketException.EXCEPTION
        event.deleteSoft()
        return eventRepository.save(event)
    }
}
