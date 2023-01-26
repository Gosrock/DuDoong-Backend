package band.gosrock.api.event.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.event.model.dto.request.CreateEventRequest;
import band.gosrock.api.event.model.dto.response.EventResponse;
import band.gosrock.api.event.model.mapper.EventMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.service.EventService;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateEventUseCase {
    private final UserUtils userUtils;
    private final HostService hostService;
    private final EventService eventService;
    private final EventMapper eventMapper;

    @Transactional
    public EventResponse execute(CreateEventRequest createEventRequest) {
        final User user = userUtils.getCurrentUser();
        final Long userId = user.getId();
        // 슈퍼 호스트 이상만 공연 생성 가능
        hostService.validateSuperHostUser(createEventRequest.getHostId(), userId);
        final Event event = eventMapper.toEntity(createEventRequest);
        return EventResponse.of(eventService.createEvent(event));
    }
}
