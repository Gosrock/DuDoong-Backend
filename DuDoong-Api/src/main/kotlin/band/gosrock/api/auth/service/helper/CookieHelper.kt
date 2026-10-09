package band.gosrock.api.auth.service.helper

import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse
import band.gosrock.common.annotation.Helper
import band.gosrock.common.helper.SpringEnvironmentHelper
import band.gosrock.common.jwt.JwtTokenProvider
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie

/**
 * 인증 쿠키 (#763). 운영(prod) 기본 동작은 그대로다: domain=.dudoong.com, SameSite=Strict, Secure, HttpOnly 없음.
 *
 * - staging: domain 을 붙이지 않는다(staging.dudoong.com 호스트 전용). 붙이면 운영 쿠키와 이름·도메인·경로가 같아 서로 덮어쓴다.
 *   SameSite=Lax (스테이징 프론트와 API 가 같은 출처)
 * - 그 밖(local·dev): domain 없음, SameSite=None (localhost 프론트가 다른 출처의 API 를 부른다)
 *
 * 설정(env)으로 바꿀 수 있다
 * - `AUTH_COOKIE_HTTP_ONLY` (기본 false): 프론트가 토큰 쿠키를 JS 로 읽는 동안은 켜지 않는다
 * - `AUTH_COOKIE_NAME_PREFIX` (기본 ""): 예) 스테이징 `stg_`
 * - `AUTH_COOKIE_DOMAIN`: 프로필 기본값 대신 쓸 domain. 빈 문자열이면 호스트 전용
 * - `AUTH_COOKIE_SAME_SITE`: 프로필 기본값 대신 쓸 SameSite (Strict / Lax / None)
 */
@Helper
class CookieHelper(
    private val springEnvironmentHelper: SpringEnvironmentHelper,
    private val jwtTokenProvider: JwtTokenProvider,
    @Value("\${auth.cookie.http-only:false}") private val httpOnly: Boolean = false,
    @Value("\${auth.cookie.name-prefix:}") private val namePrefix: String = "",
    @Value("\${auth.cookie.domain:#{null}}") private val domainOverride: String? = null,
    @Value("\${auth.cookie.same-site:#{null}}") private val sameSiteOverride: String? = null,
) {

    fun getAccessTokenName(): String = namePrefix + "accessToken"

    fun getRefreshTokenName(): String = namePrefix + "refreshToken"

    fun getAccessTokenFromRequest(request: HttpServletRequest): String? =
        getTokenCookie(request, getAccessTokenName())

    fun getRefreshTokenFromRequest(request: HttpServletRequest): String? =
        getTokenCookie(request, getRefreshTokenName())

    fun hasAccessTokenCookie(request: HttpServletRequest): Boolean =
        request.cookies?.any { it.name == getAccessTokenName() } == true

    /**
     * 이름이 같은 쿠키가 여럿이면 이 서버가 서명한 것을 쓴다 (#763).
     * 스테이징(호스트 전용 쿠키)에는 운영의 .dudoong.com 쿠키가 같은 이름으로 함께 온다. 없으면 첫 번째(기존 동작)
     */
    private fun getTokenCookie(request: HttpServletRequest, name: String): String? {
        val values = request.cookies?.filter { it.name == name }?.map { it.value }.orEmpty()
        if (values.size <= 1) return values.firstOrNull()
        return values.firstOrNull { jwtTokenProvider.isSignedByThisServer(it) } ?: values.first()
    }

    fun getTokenCookies(tokenAndUserResponse: TokenAndUserResponse): HttpHeaders {
        val accessToken = buildCookie(
            getAccessTokenName(),
            tokenAndUserResponse.accessToken,
            tokenAndUserResponse.accessTokenAge
        )
        val refreshToken = buildCookie(
            getRefreshTokenName(),
            tokenAndUserResponse.refreshToken,
            tokenAndUserResponse.refreshTokenAge
        )

        return HttpHeaders().apply {
            add(HttpHeaders.SET_COOKIE, accessToken.toString())
            add(HttpHeaders.SET_COOKIE, refreshToken.toString())
        }
    }

    fun deleteCookies(): HttpHeaders {
        val accessToken = buildCookie(getAccessTokenName(), "", 0)
        val refreshToken = buildCookie(getRefreshTokenName(), "", 0)

        return HttpHeaders().apply {
            add(HttpHeaders.SET_COOKIE, accessToken.toString())
            add(HttpHeaders.SET_COOKIE, refreshToken.toString())
        }
    }

    private fun buildCookie(name: String, value: String, maxAge: Long): ResponseCookie {
        val domain = domainOverride ?: if (springEnvironmentHelper.isProdProfile()) PROD_DOMAIN else null

        return ResponseCookie.from(name, value)
            .path("/")
            .maxAge(maxAge)
            .sameSite(sameSite())
            .secure(true)
            .httpOnly(httpOnly)
            .apply { if (!domain.isNullOrBlank()) domain(domain) }
            .build()
    }

    private fun sameSite(): String = sameSiteOverride?.takeIf { it.isNotBlank() } ?: when {
        springEnvironmentHelper.isProdProfile() -> "Strict"
        springEnvironmentHelper.isStagingProfile() -> "Lax"
        else -> "None"
    }

    companion object {
        const val PROD_DOMAIN = ".dudoong.com"
    }
}
