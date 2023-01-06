package band.gosrock.domain.common.dto;


import band.gosrock.domain.domains.user.domain.User;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ProfileViewDto {
    private final Long id;
    private final String profileImage;
    private final String name;

    @Builder
    public ProfileViewDto(
            Long id, String email, String phoneNumber, String profileImage, String name) {
        this.id = id;
        this.profileImage = profileImage;
        this.name = name;
    }

    public static ProfileViewDto from(User user) {
        return ProfileViewDto.builder()
                .id(user.getId().getValue())
                .name(user.getProfile().getName())
                .profileImage(user.getProfile().getProfileImage())
                .build();
    }
}
