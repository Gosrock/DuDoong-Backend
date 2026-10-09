package band.gosrock.api.auth.service.helper

import band.gosrock.common.annotation.Helper
import band.gosrock.common.dto.OIDCDecodePayload
import band.gosrock.common.exception.InvalidTokenException
import band.gosrock.common.jwt.JwtOIDCProvider
import band.gosrock.infrastructure.outer.api.oauth.dto.OIDCPublicKeysResponse

@Helper
class OauthOIDCHelper(
    private val jwtOIDCProvider: JwtOIDCProvider
) {

    fun getPayloadFromIdToken(
        token: String,
        iss: String,
        audiences: Set<String>,
        oidcPublicKeysResponse: OIDCPublicKeysResponse
    ): OIDCDecodePayload {
        val kid = jwtOIDCProvider.getKidFromUnsignedTokenHeader(token)

        // 공개키 목록에 없는 kid 는 잘못된 토큰 (401)
        val oidcPublicKeyDto = oidcPublicKeysResponse.keys?.firstOrNull { it.kid == kid }
            ?: throw InvalidTokenException.EXCEPTION
        val modulus = oidcPublicKeyDto.n ?: throw InvalidTokenException.EXCEPTION
        val exponent = oidcPublicKeyDto.e ?: throw InvalidTokenException.EXCEPTION

        return jwtOIDCProvider.getOIDCTokenBody(token, modulus, exponent, iss, audiences)
    }
}
