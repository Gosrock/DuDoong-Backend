package band.gosrock.events;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class DomainEvent {
    private final LocalDateTime publishAt;

    public DomainEvent() {
        this.publishAt = LocalDateTime.now();
    }
}
