package band.gosrock.api.auth.service.helper;

import static band.gosrock.common.consts.DuDoongStatic.BEARER;
import static band.gosrock.common.consts.DuDoongStatic.KAKAO_OAUTH_QUERY_STRING;

import band.gosrock.api.auth.model.dto.KakaoUserInfoDto;
import band.gosrock.api.auth.service.helper.OauthOIDCHelper;
import band.gosrock.common.annotation.Helper;
import band.gosrock.common.dto.OIDCDecodePayload;
import band.gosrock.common.properties.OauthProperties;
import band.gosrock.domain.domains.user.domain.OauthInfo;
import band.gosrock.domain.domains.user.domain.OauthProvider;
import band.gosrock.infrastructure.outer.api.client.KakaoInfoClient;
import band.gosrock.infrastructure.outer.api.client.KakaoOauthClient;
import band.gosrock.infrastructure.outer.api.dto.KakaoInformationResponse;
import band.gosrock.infrastructure.outer.api.dto.OIDCPublicKeysResponse;
import lombok.RequiredArgsConstructor;

@Helper
@RequiredArgsConstructor
public class KakaoOauthHelper {
    private final OauthProperties oauthProperties;

    private final KakaoInfoClient kakaoInfoClient;
    private final KakaoOauthClient kakaoOauthClient;

    private final OauthOIDCHelper oauthOIDCHelper;

    public String getKaKaoOauthLink() {
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

    public OIDCDecodePayload getOIDCDecodePayload(String token) {
        OIDCPublicKeysResponse oidcPublicKeysResponse = kakaoOauthClient.getKakaoOIDCOpenKeys();
        return oauthOIDCHelper.getPayloadFromIdToken(
                token,
                oauthProperties.getKakaoBaseUrl(),
                oauthProperties.getKakaoAppId(),
                oidcPublicKeysResponse);
    }

    public OauthInfo getOauthInfoByIdToken(String idToken) {
        OIDCDecodePayload oidcDecodePayload = getOIDCDecodePayload(idToken);
        return OauthInfo.builder()
                .provider(OauthProvider.KAKAO)
                .oid(oidcDecodePayload.getSub())
                .build();
    }
}
