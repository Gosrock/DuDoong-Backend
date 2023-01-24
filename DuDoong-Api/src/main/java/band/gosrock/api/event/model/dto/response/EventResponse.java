package band.gosrock.api.event.model.dto.response;


import band.gosrock.common.annotation.DateFormat;
import band.gosrock.domain.common.vo.EventDetailVo;
import band.gosrock.domain.common.vo.EventPlaceVo;
import band.gosrock.domain.domains.event.domain.Event;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventResponse {
    private Long eventId;

    private Long hostId;

    @Schema(description = "공연 이름")
    private String name;

    @Schema(description = "공연 시작 시간")
    @DateFormat
    private LocalDateTime startAt;

    @Schema(description = "공연 시작 시간")
    @DateFormat
    private LocalDateTime endAt;

    private String urlName;

    @JsonUnwrapped private EventDetailVo eventDetail;
    @JsonUnwrapped private EventPlaceVo eventPlace;

    /*************** 미확정된 정보 ******************/
    private Long runTime;

    @DateFormat private LocalDateTime ticketingStartAt;

    @DateFormat private LocalDateTime ticketingEndAt;
    /************* 미확정된 정보 ******************/

    public static EventResponse of(Event event) {
        return EventResponse.builder()
                .eventId(event.getId())
                .hostId(event.getHostId())
                .name(event.getName())
                .startAt(event.getStartAt())
                .endAt(event.getEndAt())
                .runTime(event.getRunTime())
                .eventDetail(EventDetailVo.from(event))
                .eventPlace(EventPlaceVo.from(event))
                .urlName(event.getUrlName())
                .ticketingStartAt(event.getTicketingStartAt())
                .ticketingEndAt(event.getTicketingEndAt())
                .build();
    }
}
