package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.host.domain.HostUser;
import band.gosrock.domain.domains.user.domain.User;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HostUserVo {
    @JsonUnwrapped private final UserInfoVo userInfoVo;

    private final HostRole role;

    private final Boolean active;

    public static HostUserVo from(User user, HostUser hostUser) {
        return HostUserVo.builder()
                .userInfoVo(user.toUserInfoVo())
                .active(hostUser.getActive())
                .role(hostUser.getRole())
                .build();
    }

    public static HostUserVo from(UserInfoVo userInfoVo, HostUser hostUser) {
        return HostUserVo.builder()
                .userInfoVo(userInfoVo)
                .active(hostUser.getActive())
                .role(hostUser.getRole())
                .build();
    }
}
