package band.gosrock.domain.domain.user.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OauthProvider {
    KAKAO("KAKAO");

    private String value;
}
