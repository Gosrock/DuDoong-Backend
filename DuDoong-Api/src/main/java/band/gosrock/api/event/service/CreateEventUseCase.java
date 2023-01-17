package band.gosrock.api.event.service;


import band.gosrock.api.config.security.SecurityUtils;
import band.gosrock.api.event.model.dto.request.CreateEventRequest;
import band.gosrock.api.event.model.dto.response.EventResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.service.EventService;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateEventUseCase {

    private final EventService eventService;
    private final UserDomainService userDomainService;

    @Transactional
    public EventResponse execute(CreateEventRequest createEventRequest) {
        // 존재하는 유저인지 검증
        userDomainService.retrieveUser(SecurityUtils.getCurrentUserId());
        // Todo:: 호스트 검증
        // 이벤트 생성
        Event event = eventService.createEvent(createEventRequest.toEntity());
        return EventResponse.of(event);
    }
}
