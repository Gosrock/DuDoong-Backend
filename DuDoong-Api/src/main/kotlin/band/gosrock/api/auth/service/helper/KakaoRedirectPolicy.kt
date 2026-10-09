package band.gosrock.api.auth.service.helper

import band.gosrock.common.annotation.Helper
import band.gosrock.common.helper.SpringEnvironmentHelper
import java.net.URI
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

/**
 * 카카오 로그인 redirect_uri 의 기준 주소 허용 목록 (#763). redirect_uri = `{기준 주소}/kakao/callback`.
 *
 * 요청 헤더(Referer)로는 허용 목록 중 하나를 고르기만 한다: 출처가 같고, 경로가 `/admin` 으로 시작하면(또는 로컬 호스트 어드민 포트 5173) 호스트 어드민.
 * 목록에 없으면 첫 번째 값(그 환경의 관객 앱)을 쓴다. 카카오 콘솔에 등록한 redirect_uri 와 같아야 한다.
 *
 * `auth.kakao.redirect-bases`(env `AUTH_KAKAO_REDIRECT_BASES`, 쉼표 구분)를 주면 프로필 기본값 대신 쓴다.
 */
@Helper
class KakaoRedirectPolicy(
    private val springEnvironmentHelper: SpringEnvironmentHelper,
    @Value("\${auth.kakao.redirect-bases:}") private val configuredBases: String = "",
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun allowedBases(): List<String> {
        val configured = configuredBases.split(",").map { it.trim().trimEnd('/') }.filter { it.isNotEmpty() }
        if (configured.isNotEmpty()) return configured
        return when {
            springEnvironmentHelper.isProdProfile() -> PROD_BASES
            springEnvironmentHelper.isStagingProfile() -> STAGING_BASES + LOCAL_BASES
            else -> LOCAL_BASES + STAGING_BASES
        }
    }

    fun resolveBase(referer: String?): String {
        val bases = allowedBases()
        val candidate = candidateOf(referer)
        if (candidate != null && candidate in bases) return candidate
        log.warn("[OAUTH] 허용 목록에 없는 로그인 출처 - 기본 redirect 사용. candidate={}", candidate ?: "없음")
        return bases.first()
    }

    private fun candidateOf(referer: String?): String? {
        if (referer.isNullOrBlank()) return null
        return try {
            val uri = URI(referer)
            if (uri.scheme == null || uri.host == null) return null
            val port = if (uri.port == -1) "" else ":${uri.port}"
            val origin = "${uri.scheme}://${uri.host}$port"
            val hostAdmin = (uri.path ?: "").startsWith(ADMIN_PATH) || uri.port == LOCAL_HOST_ADMIN_PORT
            if (hostAdmin) origin + ADMIN_PATH else origin
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val ADMIN_PATH = "/admin"
        private const val LOCAL_HOST_ADMIN_PORT = 5173
        val PROD_BASES = listOf("https://dudoong.com", "https://dudoong.com/admin")
        val STAGING_BASES = listOf("https://staging.dudoong.com", "https://staging.dudoong.com/admin")
        val LOCAL_BASES = listOf("http://localhost:3000", "http://localhost:5173/admin")
    }
}
