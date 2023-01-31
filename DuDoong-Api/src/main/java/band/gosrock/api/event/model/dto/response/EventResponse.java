package band.gosrock.api.event.model.dto.response;


import band.gosrock.domain.common.vo.EventBasicVo;
import band.gosrock.domain.common.vo.EventDetailVo;
import band.gosrock.domain.common.vo.EventPlaceVo;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.event.domain.EventStatus;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventResponse {
    private Long eventId;
    private Long hostId;
    private EventStatus status;

    @JsonUnwrapped private EventBasicVo eventBasic;
    @JsonUnwrapped private EventDetailVo eventDetail;
    private EventPlaceVo place;

    public static EventResponse of(Event event) {
        return EventResponse.builder()
                .eventId(event.getId())
                .hostId(event.getHostId())
                .eventBasic(EventBasicVo.from(event))
                .eventDetail(EventDetailVo.from(event))
                .place(EventPlaceVo.from(event))
                .status(event.getStatus())
                .build();
    }
}
