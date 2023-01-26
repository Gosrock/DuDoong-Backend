package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileVo {

    private final Long userId;

    private final String userName;

    private final String email;

    private final String profileImage;

    public static UserProfileVo from(User user) {
        return UserProfileVo.builder()
                .userId(user.getId())
                .userName(user.getProfile().getName())
                .email(user.getProfile().getEmail())
                .profileImage(user.getProfile().getProfileImage())
                .build();
    }
}
