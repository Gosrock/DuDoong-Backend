package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Getter;

/*
이벤트 정보 VO (공용)
 */
@Getter
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

    public EventInfoVo(Event event) {
        this.eventName = event.getName();
        this.posterImage = event.getPosterImage();
        this.startAt = event.getStartAt();
        this.eventStatus = event.getStatus();
        this.placeName = event.getPlaceName();
    }
}
