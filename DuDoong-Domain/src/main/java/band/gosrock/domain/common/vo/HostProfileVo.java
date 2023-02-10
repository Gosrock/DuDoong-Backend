package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.host.domain.Host;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HostProfileVo {
    private final Long hostId;

    private final String name;

    private final String introduce;

    private final ImageVo profileImageUrl;

    public static HostProfileVo from(Host host) {
        return HostProfileVo.builder()
                .hostId(host.getId())
                .name(host.getProfile().getName())
                .introduce(host.getProfile().getIntroduce())
                .profileImageUrl(host.getProfile().getProfileImage())
                .build();
    }
}
