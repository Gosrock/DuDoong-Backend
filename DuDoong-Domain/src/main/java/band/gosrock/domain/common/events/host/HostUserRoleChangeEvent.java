package band.gosrock.domain.common.events.host;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.host.domain.HostUser;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class HostUserRoleChangeEvent extends DomainEvent {
    private final Long hostId;
    private final String hostName;
    private final HostRole role;
    private final Long userId;

    public static HostUserRoleChangeEvent of(Host host, HostUser hostUser) {
        return HostUserRoleChangeEvent.builder()
                .hostId(host.getId())
                .hostName(host.toHostProfileVo().getName())
                .role(hostUser.getRole())
                .userId(hostUser.getUserId())
                .build();
    }
}
