package band.gosrock.api.auth.model.dto.request;


import band.gosrock.domain.domains.user.domain.Profile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RegisterRequest {
    private final String email;
    private final String phoneNumber;
    private final String profileImage;
    private final String name;
    // oauth 제공자

    public Profile toProfile() {
        return Profile.builder()
                .profileImage(this.profileImage)
                .phoneNumber(phoneNumber)
                .name(name)
                .email(email)
                .build();
    }
}
