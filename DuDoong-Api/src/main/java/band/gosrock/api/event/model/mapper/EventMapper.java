package band.gosrock.api.event.model.mapper;


import band.gosrock.api.event.model.dto.request.CreateEventRequest;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.exception.ForbiddenHostException;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class EventMapper {

    private final HostAdaptor hostAdaptor;
    private final EventAdaptor eventAdaptor;

    @Transactional(readOnly = true)
    public Event toEntity(CreateEventRequest createEventRequest, Long userId) {
        final Host host = hostAdaptor.findById(createEventRequest.getHostId());
        if (host.hasHostUserId(userId)) {
            return Event.builder()
                    .hostId(host.getId())
                    .startAt(createEventRequest.getStartAt())
                    .runTime(createEventRequest.getRunTime())
                    .latitude(createEventRequest.getLatitude())
                    .longitude(createEventRequest.getLongitude())
                    .posterImage(createEventRequest.getPosterImageUrl())
                    .name(createEventRequest.getName())
                    .url(createEventRequest.getAliasUrl())
                    .placeName(createEventRequest.getPlaceName())
                    .placeAddress(createEventRequest.getPlaceAddress())
                    .content(createEventRequest.getContent())
                    .ticketingStartAt(createEventRequest.getTicketingStartAt())
                    .ticketingEndAt(createEventRequest.getTicketingEndAt())
                    .build();
        }
        throw ForbiddenHostException.EXCEPTION;
    }
}
