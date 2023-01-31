package band.gosrock.api.event.service;


import band.gosrock.api.common.aop.HostRolesAllowed;
import band.gosrock.api.event.model.dto.request.UpdateEventDetailRequest;
import band.gosrock.api.event.model.dto.response.EventResponse;
import band.gosrock.api.event.model.mapper.EventMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.service.EventService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateEventDetailUseCase {
    private final EventService eventService;
    private final EventAdaptor eventAdaptor;
    private final EventMapper eventMapper;

    @HostRolesAllowed(role = "MANAGER")
    public EventResponse execute(Long eventId, UpdateEventDetailRequest updateEventDetailRequest) {
        final Event event = eventAdaptor.findById(eventId);
        return EventResponse.of(
                eventService.updateEventDetail(
                        event, eventMapper.toEventDetail(updateEventDetailRequest)));
    }
}
