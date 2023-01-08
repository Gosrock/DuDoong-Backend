package band.gosrock.domain.domains.user.domain;


import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile {
    private String name;
    private String email;

    private String phoneNumber;

    private String profileImage;

    @Builder
    public Profile(String name, String email, String phoneNumber, String profileImage) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.profileImage = profileImage;
    }
}
