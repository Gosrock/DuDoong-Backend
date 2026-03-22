package band.gosrock.api.auth.service.helper

import band.gosrock.api.auth.model.dto.KakaoUserInfoDto
import band.gosrock.common.annotation.Helper
import band.gosrock.common.consts.DuDoongStatic.BEARER
import band.gosrock.common.consts.DuDoongStatic.KAKAO_OAUTH_QUERY_STRING
import band.gosrock.common.dto.OIDCDecodePayload
import band.gosrock.common.properties.OauthProperties
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.OauthProvider
import band.gosrock.infrastructure.outer.api.oauth.client.KakaoInfoClient
import band.gosrock.infrastructure.outer.api.oauth.client.KakaoOauthClient
import band.gosrock.infrastructure.outer.api.oauth.dto.KakaoTokenResponse
import band.gosrock.infrastructure.outer.api.oauth.dto.UnlinkKaKaoTarget

@Helper
class KakaoOauthHelper(
    private val oauthProperties: OauthProperties,
    private val kakaoInfoClient: KakaoInfoClient,
    private val kakaoOauthClient: KakaoOauthClient,
    private val oauthOIDCHelper: OauthOIDCHelper
) {

    fun getKaKaoOauthLinkTest(): String =
        oauthProperties.getKakaoBaseUrl() + String.format(
            KAKAO_OAUTH_QUERY_STRING,
            oauthProperties.getKakaoClientId(),
            oauthProperties.getKakaoRedirectUrl()
        )

    fun getKaKaoOauthLink(referer: String): String =
        oauthProperties.getKakaoBaseUrl() + String.format(
            KAKAO_OAUTH_QUERY_STRING,
            oauthProperties.getKakaoClientId(),
            "$referer/kakao/callback"
        )

    fun getOauthToken(code: String, referer: String): KakaoTokenResponse =
        kakaoOauthClient.kakaoAuth(
            oauthProperties.getKakaoClientId(),
            "$referer/kakao/callback",
            code,
            oauthProperties.getKakaoClientSecret()
        )

    fun getOauthTokenTest(code: String): KakaoTokenResponse =
        kakaoOauthClient.kakaoAuth(
            oauthProperties.getKakaoClientId(),
            oauthProperties.getKakaoRedirectUrl(),
            code,
            oauthProperties.getKakaoClientSecret()
        )

    fun getUserInfo(oauthAccessToken: String): KakaoUserInfoDto {
        val response = kakaoInfoClient.kakaoUserInfo(BEARER + oauthAccessToken)

        return KakaoUserInfoDto(
            oauthProvider = OauthProvider.KAKAO,
            name = response.getName(),
            phoneNumber = response.getPhoneNumber(),
            profileImage = response.getProfileUrl(),
            email = response.getEmail(),
            oauthId = response.id ?: ""
        )
    }

    fun getOIDCDecodePayload(token: String): OIDCDecodePayload {
        val oidcPublicKeysResponse = kakaoOauthClient.getKakaoOIDCOpenKeys()
        return oauthOIDCHelper.getPayloadFromIdToken(
            token,
            oauthProperties.getKakaoBaseUrl(),
            oauthProperties.getKakaoAppId(),
            oidcPublicKeysResponse
        )
    }

    fun getOauthInfoByIdToken(idToken: String): OauthInfo {
        val oidcDecodePayload = getOIDCDecodePayload(idToken)
        return OauthInfo(
            provider = OauthProvider.KAKAO,
            oid = oidcDecodePayload.sub,
        )
    }

    fun unlink(oid: String) {
        val kakaoAdminKey = oauthProperties.getKakaoAdminKey()
        val unlinkKaKaoTarget = UnlinkKaKaoTarget.from(oid)
        val header = "KakaoAK $kakaoAdminKey"
        kakaoInfoClient.unlinkUser(header, unlinkKaKaoTarget)
    }
}
