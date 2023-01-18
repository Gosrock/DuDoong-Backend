package band.gosrock.api.host.model.dto.response;


import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HostDetailResponse {
    @Schema(description = "담당자 이메일")
    private final String contactEmail;

    @Schema(description = "담당자 전화번호")
    private final String contactNumber;

    //    @Schema(description = "마스터 유저의 정보")
    //    private final UserInfoVo masterUserInfoVo;
    //
    //    @Schema(description = "호스트 유저 정보")
    //    private final Set<UserInfoVo> userInfoVoList;
    // todo:: UserRole 에서 Set<User> 추출해서 VO 리스트로 넣기

    @Schema(description = "마스터 유저의 아이디")
    private final Long masterUserId;

    @Schema(description = "호스트 유저 아이디")
    private final Set<Long> userId;

    @Schema(description = "파트너쉽 여부")
    private final boolean partner;

    public static HostDetailResponse of(Host host) {
        HostDetailResponseBuilder builder = HostDetailResponse.builder();
        Set<Long> userIdList = new HashSet<>();
        host.getHostUsers()
                .forEach(
                        hostUser -> {
                            if (hostUser.getRole().equals(HostRole.SUPER_HOST)) {
                                builder.masterUserId(hostUser.getUserId());
                            } else {
                                userIdList.add(hostUser.getUserId());
                            }
                        });

        return builder.contactEmail(host.getContactEmail())
                .contactNumber(host.getContactNumber())
                .userId(userIdList)
                .partner(host.getPartner())
                .build();
    }
}
