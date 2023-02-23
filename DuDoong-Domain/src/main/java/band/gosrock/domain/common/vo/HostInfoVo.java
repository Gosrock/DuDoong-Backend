package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.host.domain.Host;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
@Builder
public class HostInfoVo {
    private final Long hostId;

    private final String name;

    private final String introduce;

    private final ImageVo profileImage;

    private final String contactEmail;

    private final String contactNumber;

    private final Boolean partner;

    public static HostInfoVo from(Host host) {
        return HostInfoVo.builder()
                .hostId(host.getId())
                .name(host.getProfile().getName())
                .introduce(host.getProfile().getIntroduce())
                .profileImage(host.getProfile().getProfileImage())
                .contactEmail(host.getProfile().getContactEmail())
                .contactNumber(host.getProfile().getContactNumber())
                .partner(host.getPartner())
                .build();
    }
}
