package band.gosrock.domain.common.events.host;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HostUserJoinEvent extends DomainEvent {
    private final Long hostId;
    private final Long userId;

    public static HostUserJoinEvent of(Long hostId, Long userId) {
        return HostUserJoinEvent.builder().hostId(hostId).userId(userId).build();
    }
}
