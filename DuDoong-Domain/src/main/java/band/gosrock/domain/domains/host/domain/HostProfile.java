package band.gosrock.domain.domains.host.domain;


import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HostProfile {
    // 호스트 이름
    private String name;

    // 간단 소개
    private String introduce;

    // 프로필 이미지 url
    private String profileImageUrl;

    // 대표자 이메일
    private String contactEmail;

    // 대표자 연락처
    @Column(length = 15)
    private String contactNumber;

    protected void updateProfile(HostProfile hostProfile) {
        this.profileImageUrl = hostProfile.getProfileImageUrl();
        this.introduce = hostProfile.getIntroduce();
        this.contactEmail = hostProfile.getContactEmail();
        this.contactNumber = hostProfile.getContactNumber();
    }

    @Builder
    public HostProfile(
            String name,
            String introduce,
            String profileImageUrl,
            String contactEmail,
            String contactNumber) {
        this.name = name;
        this.introduce = introduce;
        this.profileImageUrl = profileImageUrl;
        this.contactEmail = contactEmail;
        this.contactNumber = contactNumber;
    }
}
