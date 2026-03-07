package band.gosrock.common.dto

data class OIDCDecodePayload(
    /** issuer ex https://kauth.kakao.com */
    val iss: String,
    /** client id */
    val aud: String,
    /** oauth provider account unique id */
    val sub: String,
    val email: String,
)
