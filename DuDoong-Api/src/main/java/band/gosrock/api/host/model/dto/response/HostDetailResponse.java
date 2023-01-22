package band.gosrock.api.host.model.dto.response;


import band.gosrock.domain.common.vo.UserInfoVo;
import band.gosrock.domain.domains.host.domain.Host;
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

    @Schema(description = "마스터 유저의 정보")
    private final UserInfoVo masterHostUserInfo;

    @Schema(description = "호스트 유저 정보")
    private final Set<UserInfoVo> hostUserInfoList;

    @Schema(description = "파트너쉽 여부")
    private final boolean partner;

    public static HostDetailResponse of(Host host, Set<UserInfoVo> userInfoVoSet) {
        HostDetailResponseBuilder builder = HostDetailResponse.builder();
        Set<UserInfoVo> userInfoVoList = new HashSet<>();
        userInfoVoSet.forEach(
                userInfoVo -> {
                    if (userInfoVo.getUserId().equals(host.getMasterUserId())) {
                        builder.masterHostUserInfo(userInfoVo);
                    } else {
                        userInfoVoList.add(userInfoVo);
                    }
                });

        return builder.contactEmail(host.getContactEmail())
                .contactNumber(host.getContactNumber())
                .hostUserInfoList(userInfoVoList)
                .partner(host.getPartner())
                .build();
    }
}
