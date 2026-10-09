package band.gosrock.api.auth.service.helper

import band.gosrock.common.helper.SpringEnvironmentHelper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

/** 카카오 redirect_uri 기준 주소는 허용 목록에서만 고른다 (#763) */
@DisplayName("KakaoRedirectPolicy")
class KakaoRedirectPolicyTest {

    private fun policy(profile: String, configured: String = "") =
        KakaoRedirectPolicy(SpringEnvironmentHelper(MockEnvironment().apply { setActiveProfiles(profile) }), configured)

    @Test
    fun `prod - 관객 앱과 호스트 어드민`() {
        val p = policy("prod")
        assertEquals("https://dudoong.com", p.resolveBase("https://dudoong.com/event/1"))
        assertEquals("https://dudoong.com/admin", p.resolveBase("https://dudoong.com/admin/login"))
        assertEquals("https://dudoong.com/admin", p.resolveBase("https://dudoong.com/admin"))
    }

    @Test
    fun `prod - 허용 목록 밖(다른 출처·스테이징·localhost·없음·깨진 값)은 관객 앱 기본값`() {
        val p = policy("prod")
        listOf(
            "https://evil.example/admin", "https://dudoong.com.evil.example/", "https://staging.dudoong.com/",
            "http://localhost:3000/", null, "", "::not a url::", "http://dudoong.com/",
        ).forEach { assertEquals("https://dudoong.com", p.resolveBase(it), "referer=$it") }
    }

    @Test
    fun `staging - 스테이징과 로컬 프론트 개발 서버 (5173 은 호스트 어드민)`() {
        val p = policy("staging")
        assertEquals("https://staging.dudoong.com", p.resolveBase("https://staging.dudoong.com/"))
        assertEquals("https://staging.dudoong.com/admin", p.resolveBase("https://staging.dudoong.com/admin/x"))
        assertEquals("http://localhost:3000", p.resolveBase("http://localhost:3000/"))
        assertEquals("http://localhost:5173/admin", p.resolveBase("http://localhost:5173/"))
        assertEquals("https://staging.dudoong.com", p.resolveBase("https://dudoong.com/"))
    }

    @Test
    fun `설정값이 있으면 그 목록만`() {
        val p = policy("prod", "https://a.example,https://a.example/admin")
        assertEquals("https://a.example/admin", p.resolveBase("https://a.example/admin/"))
        assertEquals("https://a.example", p.resolveBase("https://dudoong.com/"))
    }
}
