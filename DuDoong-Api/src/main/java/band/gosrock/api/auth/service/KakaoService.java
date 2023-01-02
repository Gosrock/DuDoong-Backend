package band.gosrock.api.auth.service;

import static band.gosrock.common.consts.DuDoongStatic.BEARER;
import static band.gosrock.common.consts.DuDoongStatic.KAKAO_OAUTH_QUERY_STRING;

import band.gosrock.api.auth.model.dto.KakaoUserInfoDto;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.common.properties.OauthProperties;
import band.gosrock.domain.domains.user.domain.OauthProvider;
import band.gosrock.infrastructure.outer.api.client.KakaoInfoClient;
import band.gosrock.infrastructure.outer.api.client.KakaoOauthClient;
import band.gosrock.infrastructure.outer.api.dto.KakaoInformationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@UseCase
@RequiredArgsConstructor
public class KakaoService {
    private final OauthProperties oauthProperties;

    private final KakaoInfoClient kakaoInfoClient;
    private final KakaoOauthClient kakaoOauthClient;

    protected String getKaKaoOauthLink() {
        return oauthProperties.getKakaoBaseUrl()
                + String.format(
                        KAKAO_OAUTH_QUERY_STRING,
                        oauthProperties.getKakaoClientId(),
                        oauthProperties.getKakaoRedirectUrl());
    }

    public String getOauthAccessToken(String code) {

        return kakaoOauthClient
                .kakaoAuth(
                        oauthProperties.getKakaoClientId(),
                        oauthProperties.getKakaoRedirectUrl(),
                        code,
                        oauthProperties.getKakaoClientSecret())
                .getAccessToken();
    }

    public KakaoUserInfoDto getUserInfo(String oauthAccessToken) {
        KakaoInformationResponse response =
                kakaoInfoClient.kakaoUserInfo(BEARER + oauthAccessToken);

        return KakaoUserInfoDto.builder()
                .oauthProvider(OauthProvider.KAKAO)
                .name(response.getName())
                .phoneNumber(response.getPhoneNumber())
                .profileImage(response.getProfileUrl())
                .email(response.getEmail())
                .oauthId(response.getId())
                .build();
    }
}
