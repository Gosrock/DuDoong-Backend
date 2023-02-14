package band.gosrock.domain.common.dto;


import band.gosrock.domain.common.vo.ImageVo;
import band.gosrock.domain.domains.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ProfileViewDto {
    private final Long id;
    private final ImageVo profileImage;
    private final String name;

    @Builder
    public ProfileViewDto(
            Long id, String email, String phoneNumber, ImageVo profileImage, String name) {
        this.id = id;
        this.profileImage = profileImage;
        this.name = name;
    }

    public static ProfileViewDto from(User user) {
        return ProfileViewDto.builder()
                .id(user.getId())
                .name(user.getProfile().getName())
                .profileImage(user.getProfile().getProfileImage())
                .build();
    }
}
