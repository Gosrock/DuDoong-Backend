package band.gosrock.api.event.model.mapper

import band.gosrock.api.event.model.dto.request.CreateEventRequest
import band.gosrock.api.event.model.dto.request.UpdateEventBasicRequest
import band.gosrock.api.event.model.dto.request.UpdateEventDetailRequest
import band.gosrock.api.event.model.dto.response.EventChecklistResponse
import band.gosrock.api.event.model.dto.response.EventDetailResponse
import band.gosrock.api.event.model.dto.response.EventProfileResponse
import band.gosrock.api.event.model.dto.response.EventResponse
import band.gosrock.common.annotation.Mapper
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventBasic
import band.gosrock.domain.domains.event.domain.EventDetail
import band.gosrock.domain.domains.event.domain.EventPlace
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

@Mapper
class EventMapper(
    private val hostAdaptor: HostAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val ticketItemAdaptor: TicketItemAdaptor
) {

    fun toEntity(createEventRequest: CreateEventRequest): Event {
        return Event.builder()
            .hostId(createEventRequest.hostId)
            .name(createEventRequest.name)
            .startAt(createEventRequest.startAt)
            .runTime(createEventRequest.runTime)
            .build()
    }

    fun toEventBasic(updateEventBasicRequest: UpdateEventBasicRequest): EventBasic {
        return EventBasic.builder()
            .name(updateEventBasicRequest.name)
            .runTime(updateEventBasicRequest.runTime)
            .startAt(updateEventBasicRequest.startAt)
            .build()
    }

    fun toEventDetail(updateEventDetailRequest: UpdateEventDetailRequest): EventDetail {
        return EventDetail.builder()
            .posterImageKey(updateEventDetailRequest.posterImageKey)
            .content(updateEventDetailRequest.content)
            .build()
    }

    fun toEventPlace(updateEventBasicRequest: UpdateEventBasicRequest): EventPlace {
        return EventPlace.builder()
            .placeName(updateEventBasicRequest.placeName)
            .placeAddress(updateEventBasicRequest.placeAddress)
            .latitude(updateEventBasicRequest.latitude)
            .longitude(updateEventBasicRequest.longitude)
            .build()
    }

    fun toEventDetailResponse(host: Host, event: Event): EventDetailResponse {
        return EventDetailResponse.of(host, event)
    }

    fun toEventChecklistResponse(event: Event): EventChecklistResponse {
        val hasTicketItem = ticketItemAdaptor.existsByEventId(event.id!!)
        return EventChecklistResponse.of(event, hasTicketItem)
    }

    fun toEventProfileResponsePage(userId: Long, pageable: Pageable): Page<EventProfileResponse> {
        val hostList = hostAdaptor.findAllByHostUsers_UserId(userId)
        val hostIds = hostList.map { it.id!! }
        val eventList = eventAdaptor.findAllByHostIdIn(hostIds, pageable)
        return eventList.map { event -> toEventProfileResponse(hostList, event) }
    }

    fun toEventProfileResponseSlice(userId: Long, pageable: Pageable): Slice<EventProfileResponse> {
        val hosts = hostAdaptor.querySliceHostsByActiveUserId(userId)
        val hostIds = hosts.map { it.id!! }
        val events = eventAdaptor.querySliceEventsByHostIdIn(hostIds, pageable)
        return events.map { event -> toEventProfileResponse(hosts, event) }
    }

    fun toEventResponseSliceByStatus(status: EventStatus, pageable: Pageable): Slice<EventResponse> {
        val events = eventAdaptor.querySliceEventsByStatus(status, pageable)
        return events.map { EventResponse.of(it) }
    }

    fun toEventResponseSliceByKeyword(keyword: String?, pageable: Pageable): Slice<EventResponse> {
        val events = eventAdaptor.querySliceEventsByKeyword(keyword!!, pageable)
        return events.map { EventResponse.of(it) }
    }

    private fun toEventProfileResponse(hostList: List<Host>, event: Event): EventProfileResponse? {
        for (host in hostList) {
            if (host.id == event.hostId) return EventProfileResponse.of(host, event)
        }
        return null
    }
}
