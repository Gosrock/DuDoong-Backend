package band.gosrock.domain.common.vo;


import band.gosrock.common.annotation.DateFormat;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventStatus;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/*
이벤트 정보 VO (공용)
 */
@Getter
@Builder
public class EventInfoVo {

    /*
    이벤트 이름
     */
    private final String eventName;

    /*
    이벤트 디테일
     */
    @JsonUnwrapped private final EventDetailVo eventDetailVo;

    /*
    이벤트 시작 시간
     */
    @DateFormat private final LocalDateTime startAt;

    /*
    이벤트 시작 시간
     */
    @DateFormat private final LocalDateTime endAt;

    /*
    공연 장소
     */
    private final String placeName;

    /*
    공연 상태
     */
    private final EventStatus eventStatus;

    public static EventInfoVo from(Event event) {
        return EventInfoVo.builder()
                .eventName(event.getName())
                .eventDetailVo(EventDetailVo.from(event))
                .startAt(event.getStartAt())
                .endAt(event.getEndAt())
                .placeName(event.getPlaceName())
                .eventStatus(event.getStatus())
                .build();
    }
}
