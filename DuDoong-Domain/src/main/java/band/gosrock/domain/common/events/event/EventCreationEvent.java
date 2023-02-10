package band.gosrock.domain.common.events.event;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventCreationEvent extends DomainEvent {
    private final Long hostId;
    private final String eventName;

    public static EventCreationEvent of(Long hostId, String eventName) {
        return EventCreationEvent.builder().hostId(hostId).eventName(eventName).build();
    }
}
