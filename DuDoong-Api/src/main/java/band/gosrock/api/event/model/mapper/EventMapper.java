package band.gosrock.api.event.model.mapper;


import band.gosrock.api.event.model.dto.request.CreateEventRequest;
import band.gosrock.api.event.model.dto.request.UpdateEventBasicRequest;
import band.gosrock.api.event.model.dto.request.UpdateEventDetailRequest;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventBasic;
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
    public Event toEntity(CreateEventRequest createEventRequest) {
        return Event.builder()
                .hostId(createEventRequest.getHostId())
                .name(createEventRequest.getName())
                .startAt(createEventRequest.getStartAt())
                .runTime(createEventRequest.getRunTime())
                .build();
    }

    public EventBasic toEventBasic(UpdateEventBasicRequest updateEventBasicRequest) {
        return EventBasic.builder()
                .name(updateEventBasicRequest.getName())
                .runTime(updateEventBasicRequest.getRunTime())
                .startAt(updateEventBasicRequest.getStartAt())
                .build();
    }

    public EventDetail toEventDetail(UpdateEventDetailRequest updateEventDetailRequest) {
        return EventDetail.builder()
                .posterImage(updateEventDetailRequest.getPosterImageUrl())
                .detailImage1(updateEventDetailRequest.getDetailImageUrl().get(0))
                .detailImage2(updateEventDetailRequest.getDetailImageUrl().get(1))
                .detailImage3(updateEventDetailRequest.getDetailImageUrl().get(2))
                .content(updateEventDetailRequest.getContent())
                .build();
    }

    public EventPlace toEventPlace(UpdateEventBasicRequest updateEventBasicRequest) {
        return EventPlace.builder()
                .placeName(updateEventBasicRequest.getPlaceName())
                .placeAddress(updateEventBasicRequest.getPlaceAddress())
                .latitude(updateEventBasicRequest.getLatitude())
                .longitude(updateEventBasicRequest.getLongitude())
                .build();
    }

    // todo :: 공연 장소
}
