package band.gosrock.domain.common.events.event;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import band.gosrock.domain.domains.event.domain.Event;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventDeletionEvent extends DomainEvent {
    private final Long hostId;
    private final Long eventId;

    public static EventDeletionEvent of(Event event) {
        return EventDeletionEvent.builder()
                .hostId(event.getHostId())
                .eventId(event.getId())
                .build();
    }
}
