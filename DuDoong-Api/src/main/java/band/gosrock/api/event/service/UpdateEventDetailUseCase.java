package band.gosrock.api.event.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.event.model.dto.request.UpdateEventDetailRequest;
import band.gosrock.api.event.model.dto.response.EventResponse;
import band.gosrock.api.event.model.mapper.EventMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.service.EventService;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdateEventDetailUseCase {
    private final UserUtils userUtils;
    private final HostService hostService;
    private final EventService eventService;
    private final EventAdaptor eventAdaptor;
    private final EventMapper eventMapper;

    @Transactional
    public EventResponse execute(Long eventId, UpdateEventDetailRequest updateEventDetailRequest) {
        final User user = userUtils.getCurrentUser();
        final Long userId = user.getId();
        final Event event = eventAdaptor.findById(eventId);
        hostService.validateSuperHostUser(event.getHostId(), userId);

        return EventResponse.of(
                eventService.updateEventDetail(
                        event, eventMapper.toEventDetail(updateEventDetailRequest)));
    }
}
