package band.gosrock.api.event.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.event.model.dto.request.CreateEventRequest;
import band.gosrock.api.event.model.dto.response.EventResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventStatus;
import band.gosrock.domain.domains.event.service.EventService;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.exception.ForbiddenHostException;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateEventUseCase {

    private final HostService hostService;
    private final HostAdaptor hostAdaptor;
    private final EventService eventService;
    private final EventAdaptor eventAdaptor;
    private final UserDomainService userDomainService;

    @Transactional
    public EventResponse execute(Long hostId, CreateEventRequest createEventRequest) {
        final Long userId = SecurityUtils.getCurrentUserId();
        // 존재하는 유저인지 검증
        userDomainService.retrieveUser(userId);
        // 호스트에 속하는지 검증 후 이벤트 생성
        final Host host = hostAdaptor.findById(hostId);
        if (host.hasHostUserId(userId)) {
            Event event =
                    eventService.createEvent(
                            Event.builder()
                                    .hostId(host.getId())
                                    .startAt(createEventRequest.getStartAt())
                                    .runTime(createEventRequest.getRunTime())
                                    .latitude(createEventRequest.getLatitude())
                                    .longitude(createEventRequest.getLongitude())
                                    .posterImage(createEventRequest.getPosterImage())
                                    .name(createEventRequest.getName())
                                    .url(createEventRequest.getUrl())
                                    .placeName(createEventRequest.getPlaceName())
                                    .placeAddress(createEventRequest.getPlaceAddress())
                                    .status(EventStatus.PREPARING)
                                    .content(createEventRequest.getContent()) //
                                    .ticketingStartAt(createEventRequest.getTicketingStartAt()) //
                                    .ticketingEndAt(createEventRequest.getTicketingEndAt()) //
                                    .build());
            return EventResponse.of(event);
        }
        throw ForbiddenHostException.EXCEPTION;
    }
}
