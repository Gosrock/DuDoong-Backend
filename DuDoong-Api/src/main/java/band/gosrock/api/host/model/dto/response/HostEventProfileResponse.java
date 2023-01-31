package band.gosrock.api.host.model.dto.response;


import band.gosrock.domain.common.vo.EventProfileVo;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.domain.Host;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HostEventProfileResponse {
    private Long hostId;

    private String hostName;

    @JsonUnwrapped private EventProfileVo eventProfileVo;

    public static HostEventProfileResponse of(Host host, Event event) {
        return HostEventProfileResponse.builder()
                .hostId(host.getId())
                .hostName(host.getProfile().getName())
                .eventProfileVo(event.toEventProfileVo())
                .build();
    }
}
