package band.gosrock.api.event.model.dto.response;


import band.gosrock.common.annotation.DateFormat;
import band.gosrock.domain.common.vo.EventBasicVo;
import band.gosrock.domain.common.vo.EventDetailVo;
import band.gosrock.domain.common.vo.EventPlaceVo;
import band.gosrock.domain.common.vo.HostInfoVo;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventStatus;
import band.gosrock.domain.domains.host.domain.Host;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** 이벤트 디테일 응답 DTO */
@Getter
@Builder
public class EventDetailResponse {

    private String name;

    @DateFormat private LocalDateTime startAt;

    private Long runTime;

    @JsonUnwrapped private EventDetailVo eventDetailVo;
    @JsonUnwrapped private EventPlaceVo eventPlaceVo;

    private HostInfoVo host;
    private EventStatus status;

    public static EventDetailResponse of(Host host, Event event) {
        EventBasicVo eventBasicVo = event.toEventBasicVo();
        return EventDetailResponse.builder()
                .name(eventBasicVo.getName())
                .startAt(eventBasicVo.getStartAt())
                .runTime(eventBasicVo.getRunTime())
                .eventDetailVo(event.toEventDetailVo())
                .eventPlaceVo(event.toEventPlaceVo())
                .host(host.toHostInfoVo())
                .status(event.getStatus())
                .build();
    }
}
