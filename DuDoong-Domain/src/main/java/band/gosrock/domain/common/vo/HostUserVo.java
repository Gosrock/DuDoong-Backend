package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.user.domain.User;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HostUserVo {
    @JsonUnwrapped private final UserInfoVo userInfoVo;

    private final HostRole role;

    public static HostUserVo from(User user, HostRole role) {
        return HostUserVo.builder().userInfoVo(user.toUserInfoVo()).role(role).build();
    }

    public static HostUserVo from(UserInfoVo userInfoVo, HostRole role) {
        return HostUserVo.builder().userInfoVo(userInfoVo).role(role).build();
    }
}
