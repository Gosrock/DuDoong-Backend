package band.gosrock.api.auth.model.dto.response

import band.gosrock.infrastructure.outer.api.oauth.dto.KakaoTokenResponse

data class OauthTokenResponse(
    val accessToken: String?,
    val refreshToken: String?,
    val idToken: String?
) {
    companion object {
        fun from(kakaoTokenResponse: KakaoTokenResponse): OauthTokenResponse =
            OauthTokenResponse(
                idToken = kakaoTokenResponse.idToken,
                refreshToken = kakaoTokenResponse.refreshToken,
                accessToken = kakaoTokenResponse.accessToken
            )
    }
}
