package band.gosrock.api.auth.service.helper

import band.gosrock.common.exception.OauthStateMismatchException
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest

/** 카카오 로그인 state (#763) */
@DisplayName("OauthStateHelper")
class OauthStateHelperTest {

    private val lenient = OauthStateHelper(stateRequired = false)
    private val strict = OauthStateHelper(stateRequired = true)

    private fun request(cookie: String?) = MockHttpServletRequest().apply {
        cookie?.let { setCookies(Cookie(OauthStateHelper.COOKIE_NAME, it)) }
    }

    @Test
    fun `발급 - 매번 다른 43자 base64url, HttpOnly·Secure·Lax 쿠키 10분`() {
        val a = lenient.issue()
        assertNotEquals(a, lenient.issue())
        assertTrue(Regex("^[A-Za-z0-9_-]{43}$").matches(a))
        val cookie = lenient.stateCookie(a)[HttpHeaders.SET_COOKIE]!!.single()
        assertTrue(cookie.startsWith("oauth_state=$a;"))
        listOf("HttpOnly", "Secure", "SameSite=Lax", "Max-Age=600", "Path=/").forEach { assertTrue(cookie.contains(it), cookie) }
    }

    @Test
    fun `같으면 통과하고 쿠키를 지운다`() {
        val state = strict.issue()
        val cookie = strict.verify(request(state), state)[HttpHeaders.SET_COOKIE]!!.single()
        assertTrue(cookie.contains("Max-Age=0"), cookie)
    }

    @Test
    fun `다르면 플래그와 관계없이 거부`() {
        listOf(lenient, strict).forEach { helper ->
            val e = assertThrows<Exception> { helper.verify(request("a".repeat(43)), "b".repeat(43)) }
            assertSame(OauthStateMismatchException.EXCEPTION, e)
        }
    }

    @Test
    fun `전환 기간(플래그 꺼짐) - state 나 쿠키가 없으면 통과`() {
        assertEquals(1, lenient.verify(request(null), null)[HttpHeaders.SET_COOKIE]!!.size)
        lenient.verify(request("x"), null)
        lenient.verify(request(null), "x")
    }

    @Test
    fun `필수(플래그 켜짐) - state 나 쿠키가 없으면 거부`() {
        listOf(request(null) to null, request("x") to null, request(null) to "x", request("") to "").forEach { (req, state) ->
            assertThrows<OauthStateMismatchException> { strict.verify(req, state) }
        }
    }
}
