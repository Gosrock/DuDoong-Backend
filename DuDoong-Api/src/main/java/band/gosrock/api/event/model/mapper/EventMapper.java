package band.gosrock.api.event.model.mapper;


import band.gosrock.api.event.model.dto.request.CreateEventRequest;
import band.gosrock.api.event.model.dto.request.UpdateEventDetailRequest;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventDetail;
import band.gosrock.domain.domains.event.service.EventService;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
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
        Event event =
                Event.builder()
                        .hostId(createEventRequest.getHostId())
                        .name(createEventRequest.getName())
                        .latitude(createEventRequest.getLatitude())
                        .longitude(createEventRequest.getLongitude())
                        .placeName(createEventRequest.getPlaceName())
                        .placeAddress(createEventRequest.getPlaceAddress())
                        .urlName(createEventRequest.getUrlName())
                        .build();
        event.setTime(createEventRequest.getStartAt(), createEventRequest.getEndAt());
        return eventService.updateEventUrlName(event, createEventRequest.getUrlName());
    }

    public EventDetail toEventDetail(UpdateEventDetailRequest updateEventDetailRequest) {
        // todo :: 러닝 타임, 티켓팅 시각 정보는 추후 협의
        return EventDetail.builder()
                .posterImage(updateEventDetailRequest.getPosterImageUrl())
                .detailImage1(updateEventDetailRequest.getDetailImageUrl1())
                .detailImage2(updateEventDetailRequest.getDetailImageUrl2())
                .detailImage3(updateEventDetailRequest.getDetailImageUrl3())
                .content(updateEventDetailRequest.getContent())
                .build();
    }

    // todo :: 공연 장소
}
