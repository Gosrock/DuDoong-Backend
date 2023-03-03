package band.gosrock.api.auth.model.dto.request;


import band.gosrock.domain.domains.user.domain.Profile;
import javax.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterRequest {
    @NotEmpty private String email;
    private String phoneNumber;
    private String profileImage;
    @NotEmpty private String name;

    private Boolean marketingAgree = Boolean.FALSE;

    public Profile toProfile() {
        return Profile.builder()
                .profileImage(this.profileImage)
                .phoneNumber(phoneNumber)
                .name(name)
                .email(email)
                .build();
    }
}
