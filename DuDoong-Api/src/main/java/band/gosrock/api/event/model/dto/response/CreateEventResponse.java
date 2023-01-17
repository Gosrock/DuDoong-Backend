package band.gosrock.api.event.model.dto.response;


import band.gosrock.domain.domains.event.domain.Event;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateEventResponse {

    @Schema(description = "공연 이름")
    @NotNull(message = "공연 이름을 입력해주세요")
    private final String name;

    public static CreateEventResponse of(Event event) {
        return CreateEventResponse.builder().name(event.getName()).build();
    }
}
