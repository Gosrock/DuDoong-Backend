package band.gosrock.domain.common.vo;


import band.gosrock.common.annotation.DateFormat;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventBasic;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventBasicVo {
    private String name;
    @DateFormat private LocalDateTime startAt;
    private Long runTime;

    public static EventBasicVo from(Event event) {
        EventBasic eventBasic = event.getEventBasic();
        if (eventBasic == null) {
            return EventBasicVo.builder().build();
        }
        return EventBasicVo.builder()
                .name(eventBasic.getName())
                .startAt(eventBasic.getStartAt())
                .runTime(eventBasic.getRunTime())
                .build();
    }
}
