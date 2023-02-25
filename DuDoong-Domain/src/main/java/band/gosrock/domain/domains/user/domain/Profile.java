package band.gosrock.domain.domains.user.domain;


import band.gosrock.domain.common.vo.ImageVo;
import band.gosrock.domain.common.vo.PhoneNumberVo;
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

    @Embedded private PhoneNumberVo phoneNumberVo;
    @Embedded private ImageVo profileImage;

    public void withdraw() {
        this.name = "탈퇴한 유저";
        this.email = null;
        this.phoneNumberVo = null;
        this.profileImage = null;
    }

    @Builder
    public Profile(String name, String email, String phoneNumber, String profileImage) {
        this.name = name;
        this.email = email;
        this.phoneNumberVo = PhoneNumberVo.valueOf(phoneNumber);
        this.profileImage = ImageVo.valueOf(profileImage);
    }
}
