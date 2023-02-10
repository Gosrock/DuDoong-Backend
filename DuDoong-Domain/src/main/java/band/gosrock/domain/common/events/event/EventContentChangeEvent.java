package band.gosrock.domain.common.events.event;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import band.gosrock.domain.domains.event.domain.Event;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class EventContentChangeEvent extends DomainEvent {
    private final Long hostId;
    private final Long eventId;

    public static EventContentChangeEvent of(Event event) {
        return EventContentChangeEvent.builder()
                .hostId(event.getHostId())
                .eventId(event.getId())
                .build();
    }
}
