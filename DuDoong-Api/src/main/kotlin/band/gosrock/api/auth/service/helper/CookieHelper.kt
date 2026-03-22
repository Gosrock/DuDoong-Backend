package band.gosrock.api.auth.service.helper

import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse
import band.gosrock.common.annotation.Helper
import band.gosrock.common.helper.SpringEnvironmentHelper
import band.gosrock.common.properties.CookieProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie

@Helper
class CookieHelper(
    private val springEnvironmentHelper: SpringEnvironmentHelper,
    private val cookieProperties: CookieProperties,
) {

    fun getAccessTokenName(): String = "accessToken"

    fun getRefreshTokenName(): String = "refreshToken"

    fun getRefreshTokenFromRequest(request: HttpServletRequest): String? =
        request.cookies?.firstOrNull { it.name == getRefreshTokenName() }?.value

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
        val sameSite = if (springEnvironmentHelper.isProdProfile()) "Strict" else "None"
        val domain = cookieProperties.domain

        return ResponseCookie.from(name, value)
            .path("/")
            .maxAge(maxAge)
            .sameSite(sameSite)
            .secure(true)
            .apply {
                if (domain.isNotBlank()) {
                    domain(domain)
                }
            }
            .build()
    }
}
