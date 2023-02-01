package band.gosrock.api.event.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.event.model.dto.request.UpdateEventBasicRequest;
import band.gosrock.api.event.model.dto.response.EventResponse;
import band.gosrock.api.event.model.mapper.EventMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventBasic;
import band.gosrock.domain.domains.event.domain.EventPlace;
import band.gosrock.domain.domains.event.service.EventService;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdateEventBasicUseCase {
    private final UserUtils userUtils;
    private final HostService hostService;
    private final EventService eventService;
    private final EventAdaptor eventAdaptor;
    private final EventMapper eventMapper;

    @Transactional
    public EventResponse execute(Long eventId, UpdateEventBasicRequest updateEventBasicRequest) {
        final User user = userUtils.getCurrentUser();
        final Long userId = user.getId();
        final Event event = eventAdaptor.findById(eventId);
        hostService.validateManagerHostUser(event.getHostId(), userId);

        EventBasic eventBasic = eventMapper.toEventBasic(updateEventBasicRequest);
        EventPlace eventPlace = eventMapper.toEventPlace(updateEventBasicRequest);

        return EventResponse.of(eventService.updateEventBasic(event, eventBasic, eventPlace));
    }
}
