package band.gosrock.api.auth.model.dto.response;

import band.gosrock.infrastructure.outer.api.oauth.dto.KakaoTokenResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OauthTokenResponse {
    private String accessToken;
    private String refreshToken;
    private String idToken;

    public static OauthTokenResponse from(KakaoTokenResponse kakaoTokenResponse){
        return OauthTokenResponse.builder().idToken(kakaoTokenResponse.getIdToken()).refreshToken(
                kakaoTokenResponse.getRefreshToken())
            .accessToken(kakaoTokenResponse.getAccessToken()).build();
    }
}
