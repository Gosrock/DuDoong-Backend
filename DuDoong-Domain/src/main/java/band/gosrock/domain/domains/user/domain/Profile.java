package band.gosrock.domain.domains.user.domain;


import band.gosrock.domain.common.vo.ImageVo;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
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
    @Embedded
    private ImageVo profileImage;

    @Builder
    public Profile(String name, String email, String phoneNumber, String profileImage) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.profileImage = ImageVo.valueOf(profileImage);
    }
}
