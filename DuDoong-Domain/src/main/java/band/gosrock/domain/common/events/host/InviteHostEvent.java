package band.gosrock.domain.common.events.host;

import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import band.gosrock.domain.common.vo.HostProfileVo;
import band.gosrock.domain.domains.host.domain.Host;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InviteHostEvent extends DomainEvent {
    private final Long userId;

    private final HostProfileVo hostProfileVo;

    public static InviteHostEvent of(Host host, Long userId) {
        return InviteHostEvent.builder()
                .hostProfileVo(host.toHostProfileVo())
                .userId(userId)
                .build();
    }
}
