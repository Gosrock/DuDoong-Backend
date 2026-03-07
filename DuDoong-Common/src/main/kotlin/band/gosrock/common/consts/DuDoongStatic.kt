package band.gosrock.common.consts

object DuDoongStatic {
    const val AUTH_HEADER = "Authorization"
    const val BEARER = "Bearer "
    const val WITHDRAW_PREFIX = "DELETED:"
    const val TOKEN_ROLE = "role"
    const val TOKEN_TYPE = "type"
    const val TOKEN_ISSUER = "DuDoong"
    const val ACCESS_TOKEN = "ACCESS_TOKEN"
    const val REFRESH_TOKEN = "REFRESH_TOKEN"
    const val KR_YES = "예"
    const val KR_NO = "아니요"

    const val MILLI_TO_SECOND = 1000
    const val BAD_REQUEST = 400
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val INTERNAL_SERVER = 500

    const val NO_START_NUMBER = 1000000L
    const val MINIMUM_PAYMENT_WON = 1000L
    const val ZERO = 0L

    const val assetDomain = "https://asset.dudoong.com/"

    const val KAKAO_OAUTH_QUERY_STRING =
        "/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code"

    @JvmField
    val SwaggerPatterns = arrayOf(
        "/swagger-resources/**",
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/v3/api-docs",
    )
}
