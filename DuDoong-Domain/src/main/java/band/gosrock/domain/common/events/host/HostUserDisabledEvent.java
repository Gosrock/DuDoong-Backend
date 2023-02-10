package band.gosrock.domain.common.events.host;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostUser;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class HostUserDisabledEvent extends DomainEvent {
    private final Long hostId;
    private final String hostName;
    private final Long userId;

    public static HostUserDisabledEvent of(Host host, HostUser hostUser) {
        return HostUserDisabledEvent.builder()
                .hostId(host.getId())
                .hostName(host.toHostProfileVo().getName())
                .userId(hostUser.getUserId())
                .build();
    }
}
