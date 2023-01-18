package band.gosrock.api.event.model.dto.request;


import band.gosrock.domain.domains.event.domain.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateEventRequest {
    private String name;

    @Deprecated
    public Event toEntity() {
        return Event.builder().name(name).build();
    }
}
