package band.gosrock.api.auth.service.helper

import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse
import band.gosrock.common.annotation.Helper
import band.gosrock.common.helper.SpringEnvironmentHelper
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie

@Helper
class CookieHelper(
    private val springEnvironmentHelper: SpringEnvironmentHelper
) {

    fun getTokenCookies(tokenAndUserResponse: TokenAndUserResponse): HttpHeaders {
        val sameSite = if (springEnvironmentHelper.isProdProfile()) "Strict" else "None"

        val accessToken = ResponseCookie.from("accessToken", tokenAndUserResponse.accessToken)
            .path("/")
            .maxAge(tokenAndUserResponse.accessTokenAge)
            .sameSite(sameSite)
            .secure(true)
            .build()
        val refreshToken = ResponseCookie.from("refreshToken", tokenAndUserResponse.refreshToken)
            .path("/")
            .maxAge(tokenAndUserResponse.refreshTokenAge)
            .sameSite(sameSite)
            .secure(true)
            .build()

        return HttpHeaders().apply {
            add(HttpHeaders.SET_COOKIE, accessToken.toString())
            add(HttpHeaders.SET_COOKIE, refreshToken.toString())
        }
    }

    fun deleteCookies(): HttpHeaders {
        val sameSite = if (springEnvironmentHelper.isProdProfile()) "Strict" else "None"

        val accessToken = ResponseCookie.from("accessToken", "")
            .path("/")
            .maxAge(0)
            .sameSite(sameSite)
            .secure(true)
            .build()
        val refreshToken = ResponseCookie.from("refreshToken", "")
            .path("/")
            .maxAge(0)
            .sameSite(sameSite)
            .secure(true)
            .build()

        return HttpHeaders().apply {
            add(HttpHeaders.SET_COOKIE, accessToken.toString())
            add(HttpHeaders.SET_COOKIE, refreshToken.toString())
        }
    }
}
