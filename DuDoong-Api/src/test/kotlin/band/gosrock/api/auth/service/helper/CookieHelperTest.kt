package band.gosrock.api.auth.service.helper

import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse
import band.gosrock.common.helper.SpringEnvironmentHelper
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpHeaders

@ExtendWith(MockitoExtension::class)
@DisplayName("CookieHelper")
class CookieHelperTest {

    @Mock
    private lateinit var springEnvironmentHelper: SpringEnvironmentHelper

    @Mock
    private lateinit var request: HttpServletRequest

    private lateinit var cookieHelper: CookieHelper

    // Minimal stub for TokenAndUserResponse (no ProfileViewDto needed for cookie tests)
    private val tokenResponse = TokenAndUserResponse(
        accessToken = "access-token-value",
        accessTokenAge = 3600L,
        refreshToken = "refresh-token-value",
        refreshTokenAge = 86400L,
        userProfile = org.mockito.Mockito.mock(band.gosrock.domain.common.dto.ProfileViewDto::class.java)
    )

    @BeforeEach
    fun setUp() {
        cookieHelper = CookieHelper(springEnvironmentHelper)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun setCookiesOnRequest(vararg cookies: Cookie) {
        `when`(request.cookies).thenReturn(cookies)
    }

    /** Parse raw Set-Cookie string into a map of attribute -> value (value is "" for flags). */
    private fun parseCookieHeader(header: String): Map<String, String> {
        return header.split(";")
            .map { it.trim() }
            .associate { part ->
                val idx = part.indexOf('=')
                if (idx == -1) part to "" else part.substring(0, idx) to part.substring(idx + 1)
            }
    }

    private fun extractSetCookieHeaders(headers: HttpHeaders): List<Map<String, String>> =
        (headers[HttpHeaders.SET_COOKIE] ?: emptyList()).map { parseCookieHeader(it) }

    // ---------------------------------------------------------------------------
    // Cookie Name Tests
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("쿠키 이름 (프로파일별)")
    inner class CookieNames {

        @Test
        @DisplayName("staging 프로파일: accessToken 이름은 stg_accessToken")
        fun `staging profile returns stg_accessToken name`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(true)

            assertEquals("stg_accessToken", cookieHelper.getAccessTokenName())
        }

        @Test
        @DisplayName("staging 프로파일: refreshToken 이름은 stg_refreshToken")
        fun `staging profile returns stg_refreshToken name`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(true)

            assertEquals("stg_refreshToken", cookieHelper.getRefreshTokenName())
        }

        @Test
        @DisplayName("prod 프로파일: accessToken 이름은 accessToken (stg_ 접두사 없음)")
        fun `prod profile returns accessToken name without prefix`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)

            assertEquals("accessToken", cookieHelper.getAccessTokenName())
        }

        @Test
        @DisplayName("prod 프로파일: refreshToken 이름은 refreshToken (stg_ 접두사 없음)")
        fun `prod profile returns refreshToken name without prefix`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)

            assertEquals("refreshToken", cookieHelper.getRefreshTokenName())
        }

        @Test
        @DisplayName("local 프로파일: accessToken 이름은 accessToken (stg_ 접두사 없음)")
        fun `local profile returns accessToken name without prefix`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)

            assertEquals("accessToken", cookieHelper.getAccessTokenName())
        }

        @Test
        @DisplayName("local 프로파일: refreshToken 이름은 refreshToken (stg_ 접두사 없음)")
        fun `local profile returns refreshToken name without prefix`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)

            assertEquals("refreshToken", cookieHelper.getRefreshTokenName())
        }
    }

    // ---------------------------------------------------------------------------
    // Domain Attribute Tests
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("도메인 설정 (프로파일별)")
    inner class DomainAttribute {

        @Test
        @DisplayName("prod 프로파일: Set-Cookie에 domain=.dudoong.com 포함")
        fun `prod profile sets domain to dudoong com`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(true)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(true)

            val headers = cookieHelper.getTokenCookies(tokenResponse)
            val cookies = extractSetCookieHeaders(headers)

            cookies.forEach { attrs ->
                assertEquals(".dudoong.com", attrs["Domain"],
                    "prod 프로파일에서 Domain은 .dudoong.com 이어야 합니다")
            }
        }

        @Test
        @DisplayName("staging 프로파일: Set-Cookie에 domain=.dudoong.com 포함")
        fun `staging profile sets domain to dudoong com`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(true)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(true)

            val headers = cookieHelper.getTokenCookies(tokenResponse)
            val cookies = extractSetCookieHeaders(headers)

            cookies.forEach { attrs ->
                assertEquals(".dudoong.com", attrs["Domain"],
                    "staging 프로파일에서 Domain은 .dudoong.com 이어야 합니다")
            }
        }

        @Test
        @DisplayName("local 프로파일: Set-Cookie에 domain 속성 없음")
        fun `local profile does not set domain`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(false)

            val headers = cookieHelper.getTokenCookies(tokenResponse)
            val cookies = extractSetCookieHeaders(headers)

            cookies.forEach { attrs ->
                assertTrue(!attrs.containsKey("Domain"),
                    "local 프로파일에서는 Domain 속성이 없어야 합니다")
            }
        }
    }

    // ---------------------------------------------------------------------------
    // SameSite Tests
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("SameSite 설정 (프로파일별)")
    inner class SameSiteAttribute {

        @Test
        @DisplayName("prod 프로파일: SameSite=Strict")
        fun `prod profile uses SameSite Strict`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(true)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(true)

            val headers = cookieHelper.getTokenCookies(tokenResponse)
            val cookies = extractSetCookieHeaders(headers)

            cookies.forEach { attrs ->
                assertEquals("Strict", attrs["SameSite"],
                    "prod 프로파일에서 SameSite는 Strict여야 합니다")
            }
        }

        @Test
        @DisplayName("staging 프로파일: SameSite=None")
        fun `staging profile uses SameSite None`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(true)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(true)

            val headers = cookieHelper.getTokenCookies(tokenResponse)
            val cookies = extractSetCookieHeaders(headers)

            cookies.forEach { attrs ->
                assertEquals("None", attrs["SameSite"],
                    "staging 프로파일에서 SameSite는 None이어야 합니다")
            }
        }

        @Test
        @DisplayName("local 프로파일: SameSite=None")
        fun `local profile uses SameSite None`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(false)

            val headers = cookieHelper.getTokenCookies(tokenResponse)
            val cookies = extractSetCookieHeaders(headers)

            cookies.forEach { attrs ->
                assertEquals("None", attrs["SameSite"],
                    "local 프로파일에서 SameSite는 None이어야 합니다")
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Secure Flag Tests
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("Secure 플래그")
    inner class SecureFlag {

        @Test
        @DisplayName("모든 프로파일에서 Secure 플래그가 설정된다")
        fun `all profiles always set Secure flag`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(false)

            val headers = cookieHelper.getTokenCookies(tokenResponse)
            val cookies = extractSetCookieHeaders(headers)

            cookies.forEach { attrs ->
                assertTrue(attrs.containsKey("Secure"),
                    "모든 프로파일에서 Secure 플래그가 있어야 합니다")
            }
        }
    }

    // ---------------------------------------------------------------------------
    // getTokenCookies - cookie count and names
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("getTokenCookies 쿠키 구성")
    inner class GetTokenCookies {

        @Test
        @DisplayName("응답에 accessToken과 refreshToken 두 개의 Set-Cookie 헤더가 포함된다")
        fun `returns two Set-Cookie headers`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(false)

            val headers = cookieHelper.getTokenCookies(tokenResponse)
            val setCookies = headers[HttpHeaders.SET_COOKIE] ?: emptyList()

            assertEquals(2, setCookies.size, "Set-Cookie 헤더가 정확히 2개여야 합니다")
        }

        @Test
        @DisplayName("prod 프로파일: 쿠키 이름이 accessToken, refreshToken")
        fun `prod profile cookie names are accessToken and refreshToken`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(true)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(true)

            val headers = cookieHelper.getTokenCookies(tokenResponse)
            val cookieNames = extractSetCookieHeaders(headers).map { it.keys.first() }

            assertTrue(cookieNames.contains("accessToken"))
            assertTrue(cookieNames.contains("refreshToken"))
        }

        @Test
        @DisplayName("staging 프로파일: 쿠키 이름이 stg_accessToken, stg_refreshToken")
        fun `staging profile cookie names have stg_ prefix`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(true)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(true)

            val headers = cookieHelper.getTokenCookies(tokenResponse)
            val cookieNames = extractSetCookieHeaders(headers).map { it.keys.first() }

            assertTrue(cookieNames.contains("stg_accessToken"))
            assertTrue(cookieNames.contains("stg_refreshToken"))
        }

        @Test
        @DisplayName("쿠키에 올바른 토큰 값이 설정된다")
        fun `cookie values match token values`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(false)

            val headers = cookieHelper.getTokenCookies(tokenResponse)
            val cookies = extractSetCookieHeaders(headers)
            val valueMap = cookies.associate { it.keys.first() to it.values.first() }

            assertEquals("access-token-value", valueMap["accessToken"])
            assertEquals("refresh-token-value", valueMap["refreshToken"])
        }

        @Test
        @DisplayName("쿠키에 maxAge가 TokenAndUserResponse의 값으로 설정된다")
        fun `cookie maxAge is set from token response`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(false)

            val headers = cookieHelper.getTokenCookies(tokenResponse)
            val cookies = extractSetCookieHeaders(headers)

            val accessAttrs = cookies.first { it.keys.first() == "accessToken" }
            val refreshAttrs = cookies.first { it.keys.first() == "refreshToken" }

            assertEquals("3600", accessAttrs["Max-Age"])
            assertEquals("86400", refreshAttrs["Max-Age"])
        }

        @Test
        @DisplayName("쿠키 path는 /로 설정된다")
        fun `cookie path is root`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(false)

            val headers = cookieHelper.getTokenCookies(tokenResponse)
            val cookies = extractSetCookieHeaders(headers)

            cookies.forEach { attrs ->
                assertEquals("/", attrs["Path"], "쿠키 Path는 /여야 합니다")
            }
        }
    }

    // ---------------------------------------------------------------------------
    // deleteCookies
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteCookies")
    inner class DeleteCookies {

        @Test
        @DisplayName("두 개의 Set-Cookie 헤더가 반환된다")
        fun `returns two Set-Cookie headers`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(false)

            val headers = cookieHelper.deleteCookies()
            val setCookies = headers[HttpHeaders.SET_COOKIE] ?: emptyList()

            assertEquals(2, setCookies.size)
        }

        @Test
        @DisplayName("쿠키 값이 빈 문자열로 설정된다")
        fun `cookie values are empty string`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(false)

            val headers = cookieHelper.deleteCookies()
            val cookies = extractSetCookieHeaders(headers)
            val valueMap = cookies.associate { it.keys.first() to it.values.first() }

            assertEquals("", valueMap["accessToken"])
            assertEquals("", valueMap["refreshToken"])
        }

        @Test
        @DisplayName("maxAge=0으로 설정되어 쿠키가 즉시 만료된다")
        fun `maxAge is zero for immediate expiry`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(false)

            val headers = cookieHelper.deleteCookies()
            val cookies = extractSetCookieHeaders(headers)

            cookies.forEach { attrs ->
                assertEquals("0", attrs["Max-Age"], "삭제 쿠키의 Max-Age는 0이어야 합니다")
            }
        }

        @Test
        @DisplayName("staging 프로파일: 삭제 쿠키 이름이 stg_ 접두사를 가진다")
        fun `staging delete cookies have stg_ prefix`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(true)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(true)

            val headers = cookieHelper.deleteCookies()
            val cookieNames = extractSetCookieHeaders(headers).map { it.keys.first() }

            assertTrue(cookieNames.contains("stg_accessToken"))
            assertTrue(cookieNames.contains("stg_refreshToken"))
        }

        @Test
        @DisplayName("prod 프로파일: 삭제 쿠키에도 domain=.dudoong.com 포함")
        fun `prod delete cookies include domain`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(true)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(true)

            val headers = cookieHelper.deleteCookies()
            val cookies = extractSetCookieHeaders(headers)

            cookies.forEach { attrs ->
                assertEquals(".dudoong.com", attrs["Domain"],
                    "prod 삭제 쿠키에도 Domain이 .dudoong.com이어야 합니다")
            }
        }
    }

    // ---------------------------------------------------------------------------
    // getRefreshTokenFromRequest
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("getRefreshTokenFromRequest")
    inner class GetRefreshTokenFromRequest {

        @Test
        @DisplayName("prod 프로파일: refreshToken 이름으로 쿠키를 읽는다")
        fun `prod profile reads refreshToken cookie by name`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            setCookiesOnRequest(Cookie("refreshToken", "my-refresh-value"))

            val result = cookieHelper.getRefreshTokenFromRequest(request)

            assertEquals("my-refresh-value", result)
        }

        @Test
        @DisplayName("staging 프로파일: stg_refreshToken 이름으로 쿠키를 읽는다")
        fun `staging profile reads stg_refreshToken cookie by name`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(true)
            setCookiesOnRequest(Cookie("stg_refreshToken", "stg-refresh-value"))

            val result = cookieHelper.getRefreshTokenFromRequest(request)

            assertEquals("stg-refresh-value", result)
        }

        @Test
        @DisplayName("staging 프로파일: refreshToken 이름의 쿠키는 무시된다")
        fun `staging profile ignores non-prefixed refreshToken cookie`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(true)
            // Only provide non-prefixed cookie; staging expects stg_refreshToken
            setCookiesOnRequest(Cookie("refreshToken", "should-be-ignored"))

            val result = cookieHelper.getRefreshTokenFromRequest(request)

            assertNull(result, "staging 프로파일에서 stg_ 접두사 없는 쿠키는 무시되어야 합니다")
        }

        @Test
        @DisplayName("prod 프로파일: stg_refreshToken 쿠키는 무시된다")
        fun `prod profile ignores stg prefixed cookie`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            setCookiesOnRequest(Cookie("stg_refreshToken", "should-be-ignored"))

            val result = cookieHelper.getRefreshTokenFromRequest(request)

            assertNull(result, "prod 프로파일에서 stg_ 접두사 쿠키는 무시되어야 합니다")
        }

        @Test
        @DisplayName("요청에 쿠키가 없으면 null을 반환한다")
        fun `returns null when request has no cookies`() {
            // request.cookies returns null by default from Mockito; isStagingProfile is NOT called
            // because the safe-call ?. short-circuits before evaluating the lambda.
            `when`(request.cookies).thenReturn(null)

            val result = cookieHelper.getRefreshTokenFromRequest(request)

            assertNull(result, "쿠키가 없을 때 null을 반환해야 합니다")
        }

        @Test
        @DisplayName("요청에 다른 쿠키만 있을 때 null을 반환한다")
        fun `returns null when matching cookie is absent`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            setCookiesOnRequest(
                Cookie("someOtherCookie", "value1"),
                Cookie("anotherCookie", "value2")
            )

            val result = cookieHelper.getRefreshTokenFromRequest(request)

            assertNull(result, "refreshToken 쿠키가 없을 때 null을 반환해야 합니다")
        }

        @Test
        @DisplayName("여러 쿠키 중 올바른 refreshToken 값을 반환한다")
        fun `returns correct refreshToken value among multiple cookies`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            setCookiesOnRequest(
                Cookie("accessToken", "access-value"),
                Cookie("refreshToken", "correct-refresh"),
                Cookie("unrelated", "other")
            )

            val result = cookieHelper.getRefreshTokenFromRequest(request)

            assertEquals("correct-refresh", result)
        }

        @Test
        @DisplayName("staging: 여러 쿠키 중 stg_refreshToken 값만 반환한다")
        fun `staging returns stg_refreshToken among multiple cookies`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(true)
            setCookiesOnRequest(
                Cookie("stg_accessToken", "stg-access"),
                Cookie("refreshToken", "wrong-value"),
                Cookie("stg_refreshToken", "correct-stg-refresh")
            )

            val result = cookieHelper.getRefreshTokenFromRequest(request)

            assertEquals("correct-stg-refresh", result)
        }
    }

    // ---------------------------------------------------------------------------
    // Cross-cutting: HttpOnly is NOT set (ResponseCookie default)
    // ---------------------------------------------------------------------------

    @Nested
    @DisplayName("HttpOnly 플래그 (보안 검증)")
    inner class HttpOnlyFlag {

        @Test
        @DisplayName("쿠키에 HttpOnly 플래그가 없다 (JS 접근 가능 — 현재 구현 확인)")
        fun `cookies do not have HttpOnly flag`() {
            `when`(springEnvironmentHelper.isStagingProfile()).thenReturn(false)
            `when`(springEnvironmentHelper.isProdProfile()).thenReturn(true)
            `when`(springEnvironmentHelper.isProdAndStagingProfile()).thenReturn(true)

            val headers = cookieHelper.getTokenCookies(tokenResponse)
            val rawHeaders = headers[HttpHeaders.SET_COOKIE] ?: emptyList()

            rawHeaders.forEach { header ->
                val parts = header.split(";").map { it.trim().lowercase() }
                assertTrue(!parts.contains("httponly"),
                    "현재 구현에서 HttpOnly 플래그는 없어야 합니다 (의도적 설계 확인용 테스트)")
            }
        }
    }
}
