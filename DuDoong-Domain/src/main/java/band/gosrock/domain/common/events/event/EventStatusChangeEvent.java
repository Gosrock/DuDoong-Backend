package band.gosrock.domain.common.events.event;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import band.gosrock.domain.domains.event.domain.Event;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventStatusChangeEvent extends DomainEvent {
    private final Long hostId;
    private final Long eventId;

    public static EventStatusChangeEvent of(Event event) {
        return EventStatusChangeEvent.builder()
                .hostId(event.getHostId())
                .eventId(event.getId())
                .build();
    }
}
