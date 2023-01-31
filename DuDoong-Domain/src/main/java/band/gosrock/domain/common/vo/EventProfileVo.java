package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventProfileVo {
    private Long eventId;

    private String posterImage;

    private String name;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private String placeName;

    private EventStatus status;

    public static EventProfileVo from(Event event) {
        EventBasicVo eventBasicVo = event.toEventBasicVo();
        EventPlaceVo eventPlaceVo = event.toEventPlaceVo();
        EventDetailVo eventDetailVo = event.toEventDetailVo();

        return EventProfileVo.builder()
                .eventId(event.getId())
                .posterImage(eventDetailVo.getPosterImage())
                .name(eventBasicVo.getName())
                .startAt(eventBasicVo.getStartAt())
                .placeName(eventPlaceVo.getPlaceName())
                .status(event.getStatus())
                .endAt(event.getEndAt())
                .build();
    }
}
