package band.gosrock.api.auth.service.helper

import band.gosrock.common.annotation.Helper
import band.gosrock.common.exception.OauthStateMismatchException
import jakarta.servlet.http.HttpServletRequest
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie

/**
 * 카카오 로그인 state (#763). 링크 발급 때 추측 불가 값을 만들어 링크와 HttpOnly 쿠키에 함께 담고,
 * 코드 교환(`/oauth/kakao`) 때 프론트가 넘긴 state 와 쿠키를 비교한다. 쓰고 나면 쿠키를 지운다.
 *
 * 전환 기간: `auth.oauth.state-required`(env `AUTH_OAUTH_STATE_REQUIRED`, 기본 false) 가 꺼져 있으면
 * state 나 쿠키가 없는 요청도 받고 경고 로그만 남긴다(지금 프론트는 state 를 넘기지 않는다). 둘 다 있는데 다르면 항상 거부한다.
 */
@Helper
class OauthStateHelper(
    @Value("\${auth.oauth.state-required:false}") private val stateRequired: Boolean = false,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val random = SecureRandom()

    fun issue(): String {
        val bytes = ByteArray(STATE_BYTES).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun stateCookie(state: String): HttpHeaders = cookieHeaders(state, MAX_AGE_SECONDS)

    /** 맞으면 state 쿠키를 지우는 헤더를 돌려준다 */
    fun verify(request: HttpServletRequest, state: String?): HttpHeaders {
        val saved = request.cookies?.firstOrNull { it.name == COOKIE_NAME }?.value
        when {
            state.isNullOrBlank() -> lenientOrThrow("state 없음")
            saved.isNullOrBlank() -> lenientOrThrow("state 쿠키 없음")
            !MessageDigest.isEqual(state.toByteArray(), saved.toByteArray()) -> {
                log.warn("[OAUTH] state 불일치 - 거부")
                throw OauthStateMismatchException.EXCEPTION
            }
        }
        return cookieHeaders("", 0)
    }

    private fun lenientOrThrow(reason: String) {
        if (stateRequired) {
            log.warn("[OAUTH] {} - 거부", reason)
            throw OauthStateMismatchException.EXCEPTION
        }
        log.warn("[DEPRECATED] 카카오 로그인 state 검증 생략 - {}", reason)
    }

    // 호스트 전용(domain 없음). 카카오에서 돌아온 콜백 페이지가 같은 출처로 /oauth/kakao 를 부르므로 Lax 로 충분하다
    private fun cookieHeaders(value: String, maxAge: Long): HttpHeaders {
        val cookie = ResponseCookie.from(COOKIE_NAME, value)
            .path("/")
            .maxAge(maxAge)
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .build()
        return HttpHeaders().apply { add(HttpHeaders.SET_COOKIE, cookie.toString()) }
    }

    companion object {
        const val COOKIE_NAME = "oauth_state"
        private const val STATE_BYTES = 32
        private const val MAX_AGE_SECONDS = 600L
    }
}
