package band.gosrock.domain.common.events.user;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
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
