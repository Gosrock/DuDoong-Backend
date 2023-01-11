package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.user.domain.User;
import lombok.Getter;

@Getter
public class UserInfoVo {

    private final Long userId;

    private final String userName;

    private final String email;

    private final String phoneNumber;

    public UserInfoVo(User user) {
        this.userId = user.getId();
        this.userName = user.getProfile().getName();
        this.email = user.getProfile().getEmail();
        this.phoneNumber = user.getProfile().getPhoneNumber();
    }
}
