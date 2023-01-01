package band.gosrock.api.auth.port;


import band.gosrock.api.auth.model.dto.OauthAccessTokenResponse;
import band.gosrock.common.annotation.Port;

@Port
public interface KakaoOauthPort {
    OauthAccessTokenResponse kakaoAuth(
            String clientId, String redirectUri, String code, String client_secret);
}
