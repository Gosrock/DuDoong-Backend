package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoVo {

    private final Long userId;

    private final String userName;

    private final String email;

    private final String phoneNumber;

    private final String profileImage;

}
