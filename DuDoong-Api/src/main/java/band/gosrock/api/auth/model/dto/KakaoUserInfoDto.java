package band.gosrock.api.auth.model.dto;


import band.gosrock.domain.domains.user.domain.OauthInfo;
import band.gosrock.domain.domains.user.domain.OauthProvider;
import band.gosrock.domain.domains.user.domain.Profile;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KakaoUserInfoDto {

    // oauth인증한 사용자 고유 아이디
    private final String oauthId;

    private final String email;
    private final String phoneNumber;
    private final String profileImage;
    private final String name;
    // oauth 제공자
    private final OauthProvider oauthProvider;

    public Profile toProfile() {
        return Profile.builder().profileImage(this.profileImage).phoneNumber(phoneNumber).name(name).email(email).build();
    }

    public OauthInfo toOauthInfo() {
        return OauthInfo.builder().oid(oauthId).provider(oauthProvider).build();
    }
}
;