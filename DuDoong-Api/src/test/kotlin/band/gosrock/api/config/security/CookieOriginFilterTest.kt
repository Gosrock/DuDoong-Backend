package band.gosrock.api.config.security

import band.gosrock.api.auth.service.helper.CookieHelper
import band.gosrock.common.exception.OriginNotAllowedException
import band.gosrock.common.helper.SpringEnvironmentHelper
import band.gosrock.common.jwt.JwtTokenProvider
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

/** 쿠키 인증 상태 변경 요청의 Origin 검사 (#763). 운영 프로필 기준 */
@DisplayName("CookieOriginFilter")
class CookieOriginFilterTest {

    private val env = SpringEnvironmentHelper(MockEnvironment().apply { setActiveProfiles("prod") })
    private val filter = CookieOriginFilter(CookieHelper(env, mock(JwtTokenProvider::class.java)), WebOriginPolicy(env))

    private fun request(
        method: String = "POST",
        uri: String = "/api/v2/hosts",
        cookie: Boolean = true,
        origin: String? = null,
        referer: String? = null,
        authorization: String? = null,
    ) = MockHttpServletRequest(method, uri).apply {
        if (cookie) setCookies(Cookie("accessToken", "token"))
        origin?.let { addHeader("Origin", it) }
        referer?.let { addHeader("Referer", it) }
        authorization?.let { addHeader("Authorization", it) }
    }

    /** 다음 필터로 넘어갔는지 */
    private fun passes(request: MockHttpServletRequest): Boolean {
        val chain = MockFilterChain()
        filter.doFilter(request, MockHttpServletResponse(), chain)
        return chain.request != null
    }

    private fun rejects(request: MockHttpServletRequest) {
        val e = assertThrows<Exception> { filter.doFilter(request, MockHttpServletResponse(), MockFilterChain()) }
        assertSame(OriginNotAllowedException.EXCEPTION, e)
    }

    @ParameterizedTest
    @ValueSource(strings = ["POST", "PUT", "PATCH", "DELETE"])
    fun `쿠키 인증 + 허용 안 된 Origin 은 거부`(method: String) {
        rejects(request(method = method, origin = "https://evil.example"))
        rejects(request(method = method, origin = "http://localhost:3000")) // 운영에는 localhost 없음
    }

    @Test
    fun `쿠키 인증 + Origin·Referer 모두 없으면 거부 (서버 간 호출이라도 쿠키가 있으면)`() {
        rejects(request())
    }

    @Test
    fun `Origin 이 없으면 Referer 의 출처로 판단`() {
        assertTrue(passes(request(referer = "https://dudoong.com/admin/events/1")))
        rejects(request(referer = "https://evil.example/https://dudoong.com"))
        rejects(request(referer = "not a url"))
    }

    @Test
    fun `허용 출처면 통과`() {
        assertTrue(passes(request(origin = "https://dudoong.com")))
        assertTrue(passes(request(origin = "https://internal-admin.dudoong.com", uri = "/internal-api/v1/users/1/status", method = "PATCH")))
    }

    @Test
    fun `Authorization 헤더가 있으면 검사하지 않는다`() {
        assertTrue(passes(request(origin = "https://evil.example", authorization = "Bearer x")))
        assertTrue(passes(request(authorization = "Bearer x")))
    }

    @Test
    fun `쿠키가 없거나 GET 이면 검사하지 않는다`() {
        assertTrue(passes(request(cookie = false, origin = "https://evil.example")))
        assertTrue(passes(request(method = "GET", origin = "https://evil.example")))
    }

    @Test
    fun `토큰 재발급·로그아웃은 제외 (SSR 재발급은 Origin 없이 쿠키로 온다)`() {
        assertTrue(passes(request(uri = "/api/v1/auth/token/refresh")))
        assertTrue(passes(request(uri = "/api/v1/auth/logout")))
        rejects(request(uri = "/api/v1/auth/oauth/kakao/login"))
    }
}
