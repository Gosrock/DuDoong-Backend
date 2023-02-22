package band.gosrock.api.event.model.mapper;


import band.gosrock.api.event.model.dto.request.CreateEventRequest;
import band.gosrock.api.event.model.dto.request.UpdateEventBasicRequest;
import band.gosrock.api.event.model.dto.request.UpdateEventDetailRequest;
import band.gosrock.api.event.model.dto.response.EventChecklistResponse;
import band.gosrock.api.event.model.dto.response.EventDetailResponse;
import band.gosrock.api.event.model.dto.response.EventProfileResponse;
import band.gosrock.api.event.model.dto.response.EventResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.*;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class EventMapper {

    private final HostAdaptor hostAdaptor;
    private final EventAdaptor eventAdaptor;
    private final TicketItemAdaptor ticketItemAdaptor;

    @Transactional(readOnly = true)
    public Event toEntity(CreateEventRequest createEventRequest) {
        return Event.builder()
                .hostId(createEventRequest.getHostId())
                .name(createEventRequest.getName())
                .startAt(createEventRequest.getStartAt())
                .runTime(createEventRequest.getRunTime())
                .build();
    }

    public EventBasic toEventBasic(UpdateEventBasicRequest updateEventBasicRequest) {
        return EventBasic.builder()
                .name(updateEventBasicRequest.getName())
                .runTime(updateEventBasicRequest.getRunTime())
                .startAt(updateEventBasicRequest.getStartAt())
                .build();
    }

    public EventDetail toEventDetail(UpdateEventDetailRequest updateEventDetailRequest) {
        return EventDetail.builder()
                .posterImageKey(updateEventDetailRequest.getPosterImageKey())
                .content(updateEventDetailRequest.getContent())
                .build();
    }

    public EventPlace toEventPlace(UpdateEventBasicRequest updateEventBasicRequest) {
        return EventPlace.builder()
                .placeName(updateEventBasicRequest.getPlaceName())
                .placeAddress(updateEventBasicRequest.getPlaceAddress())
                .latitude(updateEventBasicRequest.getLatitude())
                .longitude(updateEventBasicRequest.getLongitude())
                .build();
    }

    public EventDetailResponse toEventDetailResponse(Host host, Event event) {
        return EventDetailResponse.of(host, event);
    }

    public EventChecklistResponse toEventChecklistResponse(Event event) {
        final Boolean hasTicketItem = ticketItemAdaptor.existsByEventId(event.getId());
        return EventChecklistResponse.of(event, hasTicketItem);
    }

    public Page<EventProfileResponse> toEventProfileResponsePage(Long userId, Pageable pageable) {
        List<Host> hostList = hostAdaptor.findAllByHostUsers_UserId(userId);
        List<Long> hostIds = hostList.stream().map(Host::getId).toList();
        Page<Event> eventList = eventAdaptor.findAllByHostIdIn(hostIds, pageable);
        return eventList.map(event -> this.toEventProfileResponse(hostList, event));
    }

    public Slice<EventProfileResponse> toEventProfileResponseSlice(Long userId, Pageable pageable) {
        List<Host> hosts = hostAdaptor.findAllByHostUsers_UserId(userId);
        List<Long> hostIds = hosts.stream().map(Host::getId).toList();
        Slice<Event> events = eventAdaptor.querySliceEventsByHostIdIn(hostIds, pageable);
        return events.map(event -> this.toEventProfileResponse(hosts, event));
    }

    public Slice<EventResponse> toEventResponseSliceByStatus(
            EventStatus status, Pageable pageable) {
        Slice<Event> events = eventAdaptor.querySliceEventsByStatus(status, pageable);
        return events.map(EventResponse::of);
    }

    public Slice<EventResponse> toEventResponseSliceByKeyword(String keyword, Pageable pageable) {
        Slice<Event> events = eventAdaptor.querySliceEventsByKeyword(keyword, pageable);
        return events.map(EventResponse::of);
    }

    private EventProfileResponse toEventProfileResponse(List<Host> hostList, Event event) {
        for (Host host : hostList) {
            if (host.getId().equals(event.getHostId())) return EventProfileResponse.of(host, event);
        }
        return null;
    }
}
