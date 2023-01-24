package band.gosrock.api.event.model.mapper;


import band.gosrock.api.event.model.dto.request.CreateEventRequest;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.service.EventService;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class EventMapper {

    private final HostAdaptor hostAdaptor;
    private final EventAdaptor eventAdaptor;
    private final EventService eventService;

    @Transactional(readOnly = true)
    public Event toEntity(Long userId, CreateEventRequest createEventRequest) {
        final Host host = hostAdaptor.findById(createEventRequest.getHostId());
        // 슈퍼 호스트 이상만 공연 생성 가능
        host.validateSuperHostUser(userId);
        Event event =
                Event.builder()
                        .hostId(host.getId())
                        .name(createEventRequest.getName())
                        .urlName(createEventRequest.getUrlName())
                        .build();
        event.setTime(createEventRequest.getStartAt(), createEventRequest.getEndAt());
        return eventService.setEventUrlName(event, createEventRequest.getUrlName());
    }
}
