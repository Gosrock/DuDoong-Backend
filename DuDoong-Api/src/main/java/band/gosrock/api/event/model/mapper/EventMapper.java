package band.gosrock.api.event.model.mapper;


import band.gosrock.api.event.model.dto.request.CreateEventRequest;
import band.gosrock.api.event.model.dto.request.UpdateEventDetailRequest;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventDetail;
import band.gosrock.domain.domains.event.domain.EventPlace;
import band.gosrock.domain.domains.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class EventMapper {

    private final EventService eventService;

    @Transactional(readOnly = true)
    public Event toEntity(Long userId, CreateEventRequest createEventRequest) {
        Event event =
                Event.builder()
                        .hostId(createEventRequest.getHostId())
                        .name(createEventRequest.getName())
                        .urlName(createEventRequest.getUrlName())
                        .build();
        event.setEventPlace(toEventPlace(createEventRequest));
        event.setTime(createEventRequest.getStartAt(), createEventRequest.getEndAt());
        return eventService.updateEventUrlName(event, createEventRequest.getUrlName());
    }

    public EventDetail toEventDetail(UpdateEventDetailRequest updateEventDetailRequest) {
        //        @Valid @URL String detailImage1 =
        // updateEventDetailRequest.getDetailImageUrl().get(0);
        //        @Valid @URL String detailImage2 =
        // updateEventDetailRequest.getDetailImageUrl().get(0);
        //        @Valid @URL String detailImage3 =
        // updateEventDetailRequest.getDetailImageUrl().get(0);
        // todo :: 러닝 타임, 티켓팅 시각 정보는 추후 협의
        return EventDetail.builder()
                .posterImage(updateEventDetailRequest.getPosterImageUrl())
                .detailImage1(updateEventDetailRequest.getDetailImageUrl().get(0))
                .detailImage2(updateEventDetailRequest.getDetailImageUrl().get(1))
                .detailImage3(updateEventDetailRequest.getDetailImageUrl().get(2))
                .content(updateEventDetailRequest.getContent())
                .build();
    }

    public EventPlace toEventPlace(CreateEventRequest createEventRequest) {
        return EventPlace.builder()
                .placeName(createEventRequest.getPlaceName())
                .placeAddress(createEventRequest.getPlaceAddress())
                .latitude(createEventRequest.getLatitude())
                .longitude(createEventRequest.getLongitude())
                .build();
    }

    // todo :: 공연 장소
}
