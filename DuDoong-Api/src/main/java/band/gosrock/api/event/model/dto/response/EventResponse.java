package band.gosrock.api.event.model.dto.response;


import band.gosrock.domain.domains.event.domain.Event;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventResponse {
    @Schema(description = "공연 이름")
    private final String name;

    public static EventResponse of(Event event) {
        return EventResponse.builder().name(event.getName()).build();
    }
}
