package band.gosrock.domain.common.events.event;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import band.gosrock.domain.domains.event.domain.Event;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class EventDeletionEvent extends DomainEvent {
    private final Long hostId;
    private final String eventName;

    public static EventDeletionEvent of(Event event) {
        return EventDeletionEvent.builder()
                .hostId(event.getHostId())
                .eventName(event.getEventBasic().getName())
                .build();
    }
}
