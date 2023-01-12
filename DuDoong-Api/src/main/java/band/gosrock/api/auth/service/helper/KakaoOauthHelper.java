package band.gosrock.api.auth.service.helper;

import static band.gosrock.common.consts.DuDoongStatic.BEARER;
import static band.gosrock.common.consts.DuDoongStatic.KAKAO_OAUTH_QUERY_STRING;

import band.gosrock.api.auth.model.dto.KakaoUserInfoDto;
import band.gosrock.common.annotation.Helper;
import band.gosrock.common.dto.OIDCDecodePayload;
import band.gosrock.common.properties.OauthProperties;
import band.gosrock.domain.domains.user.domain.OauthInfo;
import band.gosrock.domain.domains.user.domain.OauthProvider;
import band.gosrock.infrastructure.outer.api.oauth.client.KakaoInfoClient;
import band.gosrock.infrastructure.outer.api.oauth.client.KakaoOauthClient;
import band.gosrock.infrastructure.outer.api.oauth.dto.KakaoInformationResponse;
import band.gosrock.infrastructure.outer.api.oauth.dto.KakaoTokenResponse;
import band.gosrock.infrastructure.outer.api.oauth.dto.OIDCPublicKeysResponse;
import band.gosrock.infrastructure.outer.api.oauth.dto.UnlinkKaKaoTarget;
import lombok.RequiredArgsConstructor;

@Helper
@RequiredArgsConstructor
public class KakaoOauthHelper {
    private final OauthProperties oauthProperties;

    private final KakaoInfoClient kakaoInfoClient;
    private final KakaoOauthClient kakaoOauthClient;

    private final OauthOIDCHelper oauthOIDCHelper;

    public String getKaKaoOauthLinkTest() {
        return oauthProperties.getKakaoBaseUrl()
                + String.format(
                        KAKAO_OAUTH_QUERY_STRING,
                        oauthProperties.getKakaoClientId(),
                        oauthProperties.getKakaoRedirectUrl());
    }

    public String getKaKaoOauthLink(String referer) {
        return oauthProperties.getKakaoBaseUrl()
                + String.format(
                        KAKAO_OAUTH_QUERY_STRING,
                        oauthProperties.getKakaoClientId(),
                        referer + "kakao/callback");
    }

    public KakaoTokenResponse getOauthToken(String code, String referer) {

        return kakaoOauthClient.kakaoAuth(
                oauthProperties.getKakaoClientId(),
                referer + "kakao/callback",
                code,
                oauthProperties.getKakaoClientSecret());
    }

    public KakaoTokenResponse getOauthTokenTest(String code) {

        return kakaoOauthClient.kakaoAuth(
                oauthProperties.getKakaoClientId(),
                oauthProperties.getKakaoRedirectUrl(),
                code,
                oauthProperties.getKakaoClientSecret());
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

    public void unlink(String oid) {
        String kakaoAdminKey = oauthProperties.getKakaoAdminKey();
        UnlinkKaKaoTarget unlinkKaKaoTarget = UnlinkKaKaoTarget.from(oid);
        String header = "KakaoAK " + kakaoAdminKey;
        kakaoInfoClient.unlinkUser(header, unlinkKaKaoTarget);
    }
}
