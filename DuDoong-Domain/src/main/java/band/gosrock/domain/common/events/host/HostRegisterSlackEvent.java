package band.gosrock.domain.common.events.host;


import band.gosrock.domain.common.aop.domainEvent.DomainEvent;
import band.gosrock.domain.domains.host.domain.Host;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class HostRegisterSlackEvent extends DomainEvent {
    private final Long hostId;
    private final String hostName;

    public static HostRegisterSlackEvent of(Host host) {
        return HostRegisterSlackEvent.builder()
                .hostId(host.getId())
                .hostName(host.toHostProfileVo().getName())
                .build();
    }
}
