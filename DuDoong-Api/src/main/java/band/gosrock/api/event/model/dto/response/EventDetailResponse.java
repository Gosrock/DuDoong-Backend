package band.gosrock.api.event.model.dto.response;


import band.gosrock.domain.common.vo.EventBasicVo;
import band.gosrock.domain.common.vo.EventDetailVo;
import band.gosrock.domain.common.vo.EventPlaceVo;
import band.gosrock.domain.common.vo.HostInfoVo;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventStatus;
import band.gosrock.domain.domains.host.domain.Host;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;

/** 이벤트 디테일 응답 DTO */
@Getter
@Builder
public class EventDetailResponse {
    private EventStatus status;
    private HostInfoVo host;
    private EventPlaceVo place;
    @JsonUnwrapped private EventBasicVo eventBasicVo;
    @JsonUnwrapped private EventDetailVo eventDetailVo;

    public static EventDetailResponse of(Host host, Event event) {
        return EventDetailResponse.builder()
                .eventBasicVo(event.toEventBasicVo())
                .eventDetailVo(event.toEventDetailVo())
                .place(event.toEventPlaceVo())
                .host(host.toHostInfoVo())
                .status(event.getStatus())
                .build();
    }
}
