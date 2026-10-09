package band.gosrock.api.auth.service.helper

import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse
import band.gosrock.common.helper.SpringEnvironmentHelper
import band.gosrock.common.jwt.JwtTokenProvider
import band.gosrock.common.properties.JwtProperties
import band.gosrock.domain.common.dto.ProfileViewDto
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.http.HttpHeaders
import org.springframework.mock.env.MockEnvironment
import org.springframework.mock.web.MockHttpServletRequest

/**
 * 인증 쿠키 속성 (#763). 운영은 기존 그대로(domain=.dudoong.com, Strict), 스테이징은 호스트 전용 + Lax, 로컬은 None.
 * 프로필은 실제 [SpringEnvironmentHelper] 에 [MockEnvironment] 로 준다.
 */
@DisplayName("CookieHelper")
class CookieHelperTest {

    private val tokenResponse = TokenAndUserResponse(
        accessToken = "access-token-value",
        accessTokenAge = 3600L,
        refreshToken = "refresh-token-value",
        refreshTokenAge = 86400L,
        userProfile = mock(ProfileViewDto::class.java)
    )

    private val ownJwt = JwtTokenProvider(JwtProperties(secretKey = OWN_SECRET, accessExp = 3600, refreshExp = 86400))
    private val otherJwt = JwtTokenProvider(JwtProperties(secretKey = OTHER_SECRET, accessExp = 3600, refreshExp = 86400))

    private fun helper(
        profile: String,
        httpOnly: Boolean = false,
        prefix: String = "",
        domain: String? = null,
        sameSite: String? = null,
    ) = CookieHelper(
        SpringEnvironmentHelper(MockEnvironment().apply { setActiveProfiles(profile) }),
        ownJwt,
        httpOnly,
        prefix,
        domain,
        sameSite,
    )

    /** Set-Cookie 한 줄 → 속성 맵 (첫 항목은 이름=값, 플래그는 값 "") */
    private fun attrs(header: String): Map<String, String> =
        header.split(";").map { it.trim() }.associate { part ->
            val idx = part.indexOf('=')
            if (idx == -1) part.lowercase() to "" else part.substring(0, idx) to part.substring(idx + 1)
        }

    private fun setCookies(headers: HttpHeaders): List<Map<String, String>> =
        headers[HttpHeaders.SET_COOKIE].orEmpty().map(::attrs)

    @Nested
    @DisplayName("프로필별 기본값")
    inner class Profiles {

        @Test
        fun `prod - domain dudoong com, SameSite Strict, Secure, HttpOnly 없음 (기존 그대로)`() {
            val cookies = setCookies(helper("prod").getTokenCookies(tokenResponse))
            assertEquals(2, cookies.size)
            cookies.forEach {
                assertEquals(".dudoong.com", it["Domain"])
                assertEquals("Strict", it["SameSite"])
                assertTrue(it.containsKey("secure"))
                assertFalse(it.containsKey("httponly"))
                assertEquals("/", it["Path"])
            }
        }

        @Test
        fun `staging - domain 없음(호스트 전용), SameSite Lax`() {
            setCookies(helper("staging").getTokenCookies(tokenResponse)).forEach {
                assertFalse(it.containsKey("Domain"), "스테이징은 운영 쿠키와 겹치지 않도록 domain 을 붙이지 않는다")
                assertEquals("Lax", it["SameSite"])
                assertTrue(it.containsKey("secure"))
            }
        }

        @Test
        fun `local - domain 없음, SameSite None`() {
            setCookies(helper("local").getTokenCookies(tokenResponse)).forEach {
                assertFalse(it.containsKey("Domain"))
                assertEquals("None", it["SameSite"])
            }
        }

        @Test
        fun `이름·값·Max-Age`() {
            val cookies = setCookies(helper("prod").getTokenCookies(tokenResponse))
            assertEquals("access-token-value", cookies[0]["accessToken"])
            assertEquals("3600", cookies[0]["Max-Age"])
            assertEquals("refresh-token-value", cookies[1]["refreshToken"])
            assertEquals("86400", cookies[1]["Max-Age"])
        }

        @Test
        fun `deleteCookies - 같은 속성에 빈 값, Max-Age 0`() {
            val cookies = setCookies(helper("prod").deleteCookies())
            assertEquals(listOf("", ""), listOf(cookies[0]["accessToken"], cookies[1]["refreshToken"]))
            cookies.forEach {
                assertEquals("0", it["Max-Age"])
                assertEquals(".dudoong.com", it["Domain"])
            }
        }
    }

    @Nested
    @DisplayName("설정으로 바꾸기")
    inner class Overrides {

        @Test
        fun `HttpOnly 플래그를 켜면 두 쿠키 모두 HttpOnly`() {
            setCookies(helper("prod", httpOnly = true).getTokenCookies(tokenResponse)).forEach {
                assertTrue(it.containsKey("httponly"))
            }
        }

        @Test
        fun `이름 접두사`() {
            val h = helper("staging", prefix = "stg_")
            assertEquals("stg_accessToken", h.getAccessTokenName())
            assertEquals("stg_refreshToken", h.getRefreshTokenName())
            val names = setCookies(h.getTokenCookies(tokenResponse)).map { it.keys.first() }
            assertEquals(listOf("stg_accessToken", "stg_refreshToken"), names)
        }

        @Test
        fun `domain·SameSite 직접 지정 - 빈 domain 은 호스트 전용`() {
            setCookies(helper("staging", domain = ".dudoong.com", sameSite = "Strict").getTokenCookies(tokenResponse)).forEach {
                assertEquals(".dudoong.com", it["Domain"])
                assertEquals("Strict", it["SameSite"])
            }
            setCookies(helper("prod", domain = "").getTokenCookies(tokenResponse)).forEach {
                assertFalse(it.containsKey("Domain"))
            }
        }
    }

    @Nested
    @DisplayName("요청 쿠키 읽기")
    inner class ReadCookies {

        private fun request(vararg cookies: Cookie) = MockHttpServletRequest().apply { setCookies(*cookies) }

        @Test
        fun `없으면 null, 하나면 그 값`() {
            val h = helper("staging")
            assertNull(h.getRefreshTokenFromRequest(MockHttpServletRequest()))
            assertNull(h.getRefreshTokenFromRequest(request(Cookie("other", "x"))))
            assertEquals("r1", h.getRefreshTokenFromRequest(request(Cookie("accessToken", "a"), Cookie("refreshToken", "r1"))))
            assertTrue(h.hasAccessTokenCookie(request(Cookie("accessToken", "a"))))
            assertFalse(h.hasAccessTokenCookie(request(Cookie("refreshToken", "r"))))
        }

        @Test
        fun `같은 이름이 여럿이면 이 서버가 서명한 것을 고른다 (스테이징에 운영 쿠키가 함께 오는 경우)`() {
            val h = helper("staging")
            val other = otherJwt.generateAccessToken(1L)
            val own = ownJwt.generateAccessToken(2L)
            assertEquals(own, h.getAccessTokenFromRequest(request(Cookie("accessToken", other), Cookie("accessToken", own))))

            val otherRefresh = otherJwt.generateRefreshToken(1L)
            val ownRefresh = ownJwt.generateRefreshToken(2L)
            assertEquals(ownRefresh, h.getRefreshTokenFromRequest(request(Cookie("refreshToken", otherRefresh), Cookie("refreshToken", ownRefresh))))
        }

        @Test
        fun `여럿인데 이 서버 것이 없으면 첫 번째 (기존 동작)`() {
            val h = helper("staging")
            val first = otherJwt.generateAccessToken(1L)
            assertEquals(first, h.getAccessTokenFromRequest(request(Cookie("accessToken", first), Cookie("accessToken", "garbage"))))
        }
    }

    companion object {
        private const val OWN_SECRET = "own-secret-key-for-cookie-helper-test-0123456789"
        private const val OTHER_SECRET = "other-secret-key-for-cookie-helper-test-987654321"
    }
}
