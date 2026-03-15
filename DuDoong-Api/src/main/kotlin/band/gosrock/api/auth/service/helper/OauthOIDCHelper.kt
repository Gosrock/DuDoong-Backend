package band.gosrock.api.auth.service.helper

import band.gosrock.common.annotation.Helper
import band.gosrock.common.dto.OIDCDecodePayload
import band.gosrock.common.jwt.JwtOIDCProvider
import band.gosrock.infrastructure.outer.api.oauth.dto.OIDCPublicKeysResponse

@Helper
class OauthOIDCHelper(
    private val jwtOIDCProvider: JwtOIDCProvider
) {

    private fun getKidFromUnsignedIdToken(token: String, iss: String, aud: String): String =
        jwtOIDCProvider.getKidFromUnsignedTokenHeader(token, iss, aud)

    fun getPayloadFromIdToken(
        token: String,
        iss: String,
        aud: String,
        oidcPublicKeysResponse: OIDCPublicKeysResponse
    ): OIDCDecodePayload {
        val kid = getKidFromUnsignedIdToken(token, iss, aud)

        val oidcPublicKeyDto = oidcPublicKeysResponse.keys!!.first { it.kid == kid }

        return jwtOIDCProvider.getOIDCTokenBody(token, oidcPublicKeyDto.n!!, oidcPublicKeyDto.e!!)
    }
}
