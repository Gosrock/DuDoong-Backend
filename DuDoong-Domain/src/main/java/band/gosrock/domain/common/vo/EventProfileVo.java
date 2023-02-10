package band.gosrock.domain.common.vo;


import band.gosrock.common.annotation.DateFormat;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventProfileVo {
    private Long eventId;

    private ImageVo posterImage;

    private String name;

    @DateFormat private LocalDateTime startAt;

    @DateFormat private LocalDateTime endAt;

    private Long runTime;

    private String placeName;

    private EventStatus status;

    public static EventProfileVo from(Event event) {
        EventBasicVo eventBasicVo = event.toEventBasicVo();
        EventPlaceVo eventPlaceVo = event.toEventPlaceVo();
        EventDetailVo eventDetailVo = event.toEventDetailVo();

        return EventProfileVo.builder()
                .eventId(event.getId())
                .posterImage(eventDetailVo.getPosterImageKey())
                .name(eventBasicVo.getName())
                .startAt(eventBasicVo.getStartAt())
                .endAt(event.getEndAt())
                .runTime(eventBasicVo.getRunTime())
                .placeName(eventPlaceVo.getPlaceName())
                .status(event.getStatus())
                .build();
    }
}
