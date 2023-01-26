package band.gosrock.api.event.model.dto.response;


import band.gosrock.common.annotation.DateFormat;
import band.gosrock.domain.common.vo.EventBasicVo;
import band.gosrock.domain.common.vo.EventDetailVo;
import band.gosrock.domain.common.vo.EventPlaceVo;
import band.gosrock.domain.domains.event.domain.Event;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventResponse {
    private Long eventId;

    private Long hostId;

    @JsonUnwrapped private EventBasicVo eventBasic;
    @JsonUnwrapped private EventDetailVo eventDetail;
    @JsonUnwrapped private EventPlaceVo eventPlace;

    /*************** 미확정된 정보 ******************/
    @DateFormat private LocalDateTime ticketingStartAt;

    @DateFormat private LocalDateTime ticketingEndAt;
    /************* 미확정된 정보 ******************/

    public static EventResponse of(Event event) {
        return EventResponse.builder()
                .eventId(event.getId())
                .hostId(event.getHostId())
                .eventBasic(EventBasicVo.from(event))
                .eventDetail(EventDetailVo.from(event))
                .eventPlace(EventPlaceVo.from(event))
                .ticketingStartAt(event.getTicketingStartAt())
                .ticketingEndAt(event.getTicketingEndAt())
                .build();
    }
}
