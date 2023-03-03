package band.gosrock.api.event.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER;

import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.event.model.dto.request.UpdateEventStatusRequest;
import band.gosrock.api.event.model.dto.response.EventResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventStatus;
import band.gosrock.domain.domains.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdateEventStatusUseCase {
    private final EventService eventService;
    private final EventAdaptor eventAdaptor;

    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = EVENT_ID)
    public EventResponse execute(Long eventId, UpdateEventStatusRequest updateEventStatusRequest) {
        final Event event = eventAdaptor.findById(eventId);
        final EventStatus status = updateEventStatusRequest.getStatus();

        return EventResponse.of(eventService.updateEventStatus(event, status));
    }
}
