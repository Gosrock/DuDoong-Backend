package band.gosrock.events.user;

import band.gosrock.events.DomainEvent;
import lombok.Builder;
import lombok.Getter;

@Getter
public class UserRegisterEvent extends DomainEvent {
    private final Long userId;

    @Builder
    public UserRegisterEvent(Long userId) {
        this.userId = userId;
    }
}
