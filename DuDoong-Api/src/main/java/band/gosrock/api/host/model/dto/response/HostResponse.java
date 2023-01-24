package band.gosrock.api.host.model.dto.response;


import band.gosrock.domain.common.vo.HostInfoVo;
import band.gosrock.domain.domains.host.domain.Host;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HostResponse {
    @Schema(description = "호스트 프로필")
    @JsonUnwrapped
    private final HostInfoVo profile;

    @Schema(description = "마스터 유저의 고유 아이디")
    private final Long masterUserId;

    @Schema(description = "파트너쉽 여부")
    private final boolean partner;

    public static HostResponse of(Host host) {
        return HostResponse.builder()
                .profile(HostInfoVo.from(host))
                .masterUserId(host.getMasterUserId())
                .partner(host.getPartner())
                .build();
    }
}
