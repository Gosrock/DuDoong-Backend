package band.gosrock.api.config.security

import band.gosrock.common.helper.SpringEnvironmentHelper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment
import org.springframework.web.servlet.config.annotation.CorsRegistry

/** 프로필별 허용 출처 (#763). 운영에는 localhost·스테이징 출처가 없다 */
@DisplayName("WebOriginPolicy / CorsConfig")
class WebOriginPolicyTest {

    private fun policy(profile: String, configured: String = "") =
        WebOriginPolicy(SpringEnvironmentHelper(MockEnvironment().apply { setActiveProfiles(profile) }), configured)

    @Test
    fun `prod - 운영 프론트 출처만`() {
        val origins = policy("prod").allowedOrigins()
        assertEquals(listOf("https://dudoong.com", "https://internal-admin.dudoong.com"), origins)
        assertTrue(origins.none { it.contains("localhost") || it.contains("staging") })
    }

    @Test
    fun `staging - 스테이징 프론트와 로컬 프론트 개발 서버, 운영 출처 없음`() {
        val origins = policy("staging").allowedOrigins()
        assertTrue("https://staging.dudoong.com" in origins)
        assertTrue("http://localhost:3000" in origins && "http://localhost:5173" in origins)
        assertFalse("https://dudoong.com" in origins)
    }

    @Test
    fun `local - 전부`() {
        val origins = policy("local").allowedOrigins()
        assertTrue("https://dudoong.com" in origins && "http://localhost:3000" in origins)
    }

    @Test
    fun `설정값이 있으면 프로필 기본값 대신 쓴다 (끝 슬래시 무시)`() {
        val p = policy("prod", " https://a.example/ ,https://b.example")
        assertEquals(listOf("https://a.example", "https://b.example"), p.allowedOrigins())
        assertTrue(p.isAllowed("https://a.example"))
        assertFalse(p.isAllowed("https://dudoong.com"))
        assertFalse(p.isAllowed(null))
    }

    @Test
    fun `isAllowed - 정확히 같은 출처만 (접두·접미 비슷한 출처 거부)`() {
        val p = policy("prod")
        assertTrue(p.isAllowed("https://dudoong.com"))
        assertFalse(p.isAllowed("https://dudoong.com.evil.example"))
        assertFalse(p.isAllowed("https://evil-dudoong.com"))
        assertFalse(p.isAllowed("http://dudoong.com"))
        assertFalse(p.isAllowed("null"))
    }

    @Test
    fun `CorsConfig 는 운영 프로필에서 localhost 를 허용하지 않는다`() {
        val registry = object : CorsRegistry() {
            fun configs() = corsConfigurations
        }
        CorsConfig(policy("prod")).addCorsMappings(registry)
        val config = registry.configs()["/**"]!!
        assertEquals(listOf("https://dudoong.com", "https://internal-admin.dudoong.com"), config.allowedOrigins)
        assertEquals(null, config.checkOrigin("http://localhost:3000"))
        assertEquals("https://dudoong.com", config.checkOrigin("https://dudoong.com"))
        assertEquals(true, config.allowCredentials)
    }
}
