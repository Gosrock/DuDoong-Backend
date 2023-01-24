package band.gosrock.api.event.model.dto.response;


import band.gosrock.common.annotation.DateFormat;
import band.gosrock.domain.domains.event.domain.Event;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventResponse {
    private Long hostId;

    @Schema(description = "공연 이름")
    private String name;

    @Schema(description = "공연 시작 시간")
    @DateFormat
    private LocalDateTime startAt;

    @Schema(description = "공연 시작 시간")
    @DateFormat
    private LocalDateTime endAt;

    // 분 단위입니다
    private Long runTime;

    private Double latitude;

    private Double longitude;

    private String posterImage;

    private String urlName;

    private String placeName;

    private String placeAddress;

    private String content;

    @DateFormat
    private LocalDateTime ticketingStartAt;

    @DateFormat
    private LocalDateTime ticketingEndAt;

    public static EventResponse of(Event event) {
        return EventResponse.builder()
                .hostId(event.getHostId())
                .name(event.getName())
                .startAt(event.getStartAt())
                .endAt(event.getEndAt())
                .runTime(event.getRunTime())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .posterImage(event.getPosterImage())
                .urlName(event.getUrlName())
                .placeName(event.getPlaceName())
                .placeAddress(event.getPlaceAddress())
                .content(event.getContent())
                .ticketingStartAt(event.getTicketingStartAt())
                .ticketingEndAt(event.getTicketingEndAt())
                .build();
    }
}
