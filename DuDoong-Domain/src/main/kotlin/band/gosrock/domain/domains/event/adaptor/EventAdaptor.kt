package band.gosrock.domain.domains.event.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.exception.EventNotFoundException
import band.gosrock.domain.domains.event.repository.EventRepository
import java.time.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

@Adaptor
class EventAdaptor(private val eventRepository: EventRepository) {

    fun findById(eventId: Long): Event =
        eventRepository.findById(eventId).orElseThrow { EventNotFoundException.EXCEPTION }

    fun findAllByHostId(hostId: Long, pageable: Pageable): Page<Event> =
        eventRepository.findAllByHostId(hostId, pageable)

    fun findAllByHostIdIn(hostId: List<Long>, pageable: Pageable): Page<Event> =
        eventRepository.findAllByHostIdIn(hostId, pageable)

    fun querySliceEventsByHostIdIn(hostId: List<Long>, pageable: Pageable): Slice<Event> =
        eventRepository.querySliceEventsByHostIdIn(hostId, pageable)

    fun querySliceEventsByStatus(status: EventStatus, pageable: Pageable): Slice<Event> =
        eventRepository.querySliceEventsByStatus(status, pageable)

    fun querySliceEventsByKeyword(keyword: String, pageable: Pageable): Slice<Event> =
        eventRepository.querySliceEventsByKeyword(keyword, pageable)

    fun queryEventsByEndAtBeforeAndStatusOpen(time: LocalDateTime): List<Event> =
        eventRepository.queryEventsByEndAtBeforeAndStatusOpen(time)

    fun findAllByIds(ids: List<Long>): List<Event> =
        eventRepository.findAllByIdIn(ids)
}
