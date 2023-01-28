package band.gosrock.api.host.model.dto.response;


import band.gosrock.domain.common.vo.HostInfoVo;
import band.gosrock.domain.common.vo.HostUserVo;
import band.gosrock.domain.domains.host.domain.Host;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HostDetailResponse {
    @Schema(description = "호스트 정보")
    @JsonUnwrapped
    private final HostInfoVo hostInfo;

    @Schema(description = "마스터 유저의 정보")
    private final HostUserVo masterUser;

    @Schema(description = "호스트 유저 정보")
    private final Set<HostUserVo> hostUsers;

    @Schema(description = "슬랙 알람 url")
    private final String slackUrl;

    @Schema(description = "파트너쉽 여부")
    private final boolean partner;

    // todo:: 이벤트 정보도 묶어서 내보내기

    public static HostDetailResponse of(Host host, Set<HostUserVo> hostUserVoSet) {
        HostDetailResponseBuilder builder = HostDetailResponse.builder();
        Set<HostUserVo> hostUserVoList = new HashSet<>();
        hostUserVoSet.forEach(
                hostUserVo -> {
                    if (hostUserVo.getUserInfoVo().getUserId().equals(host.getMasterUserId())) {
                        builder.masterUser(hostUserVo);
                    } else {
                        hostUserVoList.add(hostUserVo);
                    }
                });

        return builder.hostInfo(HostInfoVo.from(host))
                .hostUsers(hostUserVoList)
                .partner(host.getPartner())
                .slackUrl(host.getSlackUrl())
                .build();
    }
}
