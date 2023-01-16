package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
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
    이벤트 이미지
     */
    private final String posterImage;

    /*
    이벤트 시작 시간
     */
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd hh:mm:ss",
            timezone = "Asia/Seoul")
    private final LocalDateTime startAt;

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
                .posterImage(event.getPosterImage())
                .startAt(event.getStartAt())
                .placeName(event.getPlaceName())
                .eventStatus(event.getStatus())
                .build();
    }
}
