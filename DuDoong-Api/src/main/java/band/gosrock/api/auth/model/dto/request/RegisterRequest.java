package band.gosrock.api.auth.model.dto.request;


import band.gosrock.domain.domains.user.domain.Profile;
import javax.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RegisterRequest {
    @NotEmpty private final String email;
    private final String phoneNumber;
    private final String profileImage;
    @NotEmpty private final String name;

    private final Boolean marketingAgree = Boolean.FALSE;

    public Profile toProfile() {
        return Profile.builder()
                .profileImage(this.profileImage)
                .phoneNumber(phoneNumber)
                .name(name)
                .email(email)
                .build();
    }
}
