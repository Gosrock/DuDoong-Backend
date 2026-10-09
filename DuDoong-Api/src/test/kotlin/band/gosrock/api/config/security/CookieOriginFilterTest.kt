package band.gosrock.api.config.security

import band.gosrock.api.auth.service.helper.CookieHelper
import band.gosrock.common.exception.OriginNotAllowedException
import band.gosrock.common.helper.SpringEnvironmentHelper
import band.gosrock.common.jwt.JwtTokenProvider
import jakarta.servlet.http.Cookie
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.slf4j.LoggerFactory
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

/** 쿠키 인증 상태 변경 요청의 Origin 검사 (#763). 운영 프로필 기준. 기본은 차단(enforce=true) 모드로 보고, report-only 는 따로 */
@DisplayName("CookieOriginFilter")
class CookieOriginFilterTest {

    private val env = SpringEnvironmentHelper(MockEnvironment().apply { setActiveProfiles("prod") })
    private val cookieHelper = CookieHelper(env, mock(JwtTokenProvider::class.java))
    private val filter = CookieOriginFilter(cookieHelper, WebOriginPolicy(env), enforce = true)
    private val reportOnly = CookieOriginFilter(cookieHelper, WebOriginPolicy(env), enforce = false)

    private fun request(
        method: String = "POST",
        uri: String = "/api/v2/hosts",
        cookie: Boolean = true,
        origin: String? = null,
        referer: String? = null,
        authorization: String? = null,
        adminToken: String? = null,
    ) = MockHttpServletRequest(method, uri).apply {
        if (cookie) setCookies(Cookie("accessToken", "token"))
        origin?.let { addHeader("Origin", it) }
        referer?.let { addHeader("Referer", it) }
        authorization?.let { addHeader("Authorization", it) }
        adminToken?.let { addHeader("X-Admin-Token", it) }
    }

    /** 다음 필터로 넘어갔는지 */
    private fun passes(request: MockHttpServletRequest, target: CookieOriginFilter = filter): Boolean {
        val chain = MockFilterChain()
        target.doFilter(request, MockHttpServletResponse(), chain)
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
    fun `Bearer 헤더가 있으면 인증이 헤더 토큰으로 되므로 검사하지 않는다 (CORS 단계 출처 거부는 별개)`() {
        assertTrue(passes(request(origin = "https://evil.example", authorization = "Bearer x")))
        assertTrue(passes(request(authorization = "Bearer x")))
    }

    @Test
    fun `Bearer 가 아닌 Authorization 이나 레거시 X-Admin-Token 은 면제하지 않는다`() {
        rejects(request(authorization = "Basic abc"))
        rejects(request(authorization = "Bearer "))
        rejects(request(adminToken = "x"))
    }

    @Test
    fun `report-only(기본값) - 막지 않고 would-reject 경고만 남긴다 (토큰 값 없음)`() {
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        val logger = LoggerFactory.getLogger(CookieOriginFilter::class.java) as Logger
        logger.addAppender(appender)
        try {
            assertTrue(passes(request(origin = "https://evil.example"), reportOnly))
            assertTrue(passes(request(), reportOnly))
            assertTrue(passes(request(origin = "https://dudoong.com"), reportOnly))
            val logs = appender.list.map { it.formattedMessage }
            assertEquals(2, logs.count { it.startsWith("[AUTH] origin-check would-reject") }, "$logs")
            assertTrue(logs.none { it.contains("token") }, "$logs")
            assertTrue(logs.first().contains("origin=https://evil.example"), "$logs")
        } finally {
            logger.detachAppender(appender)
        }
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
