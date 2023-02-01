package band.gosrock.domain.common.vo;


import band.gosrock.domain.domains.user.domain.User;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoVo {

    private final Long userId;

    private final String userName;

    private final String email;

    private final String phoneNumber;

    private final ImageVo profileImage;

    private final LocalDateTime createdAt;

    public static UserInfoVo from(User user) {
        return UserInfoVo.builder()
                .userId(user.getId())
                .userName(user.getProfile().getName())
                .email(user.getProfile().getEmail())
                .profileImage(user.getProfile().getProfileImage())
                .phoneNumber(user.getProfile().getPhoneNumber())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
