package band.gosrock.domain.common.events.host;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import band.gosrock.domain.common.vo.HostProfileVo;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.host.domain.HostUser;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class HostUserInvitationEvent extends DomainEvent {
    private final Long userId;
    private final HostRole role;
    private final HostProfileVo hostProfileVo;

    public static HostUserInvitationEvent of(Host host, HostUser hostUser) {
        return HostUserInvitationEvent.builder()
                .hostProfileVo(host.toHostProfileVo())
                .role(hostUser.getRole())
                .userId(hostUser.getUserId())
                .build();
    }
}
