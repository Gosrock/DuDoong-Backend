package band.gosrock.api.config.security

import band.gosrock.common.helper.SpringEnvironmentHelper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 브라우저 요청을 받을 프론트 출처 목록 (#763). CORS 허용 출처와 쿠키 인증 상태 변경 요청의 Origin 검사에 같이 쓴다.
 *
 * - prod: 운영 프론트만. 관객 앱·호스트 어드민(`/admin/`)·내부 어드민(`/internal-admin/`)은 모두 dudoong.com 아래에서 API 와 같은 출처로 뜨고,
 *   내부 어드민은 internal-admin.dudoong.com 으로도 열린다
 * - staging: 스테이징 프론트 + 로컬 프론트 개발 서버(localhost:3000·5173 은 스테이징 API 를 부른다).
 *   스테이징 내부 어드민은 `staging.dudoong.com/internal-admin/` 경로만 쓴다(쿠키가 호스트 전용이라 staging-internal-admin 서브도메인에는 로그인 쿠키가 가지 않는다)
 * - 그 밖(local·dev·테스트): 위 전부
 *
 * `auth.allowed-origins`(env `AUTH_ALLOWED_ORIGINS`, 쉼표 구분)를 주면 프로필 기본값 대신 그 목록을 쓴다.
 */
@Component
class WebOriginPolicy(
    private val springEnvironmentHelper: SpringEnvironmentHelper,
    @Value("\${auth.allowed-origins:}") private val configuredOrigins: String = "",
) {

    fun allowedOrigins(): List<String> {
        val configured = configuredOrigins.split(",").map { it.trim().trimEnd('/') }.filter { it.isNotEmpty() }
        if (configured.isNotEmpty()) return configured
        return when {
            springEnvironmentHelper.isProdProfile() -> PROD_ORIGINS
            springEnvironmentHelper.isStagingProfile() -> STAGING_ORIGINS
            else -> PROD_ORIGINS + STAGING_ORIGINS
        }
    }

    fun isAllowed(origin: String?): Boolean = origin != null && origin.trimEnd('/') in allowedOrigins()

    companion object {
        val PROD_ORIGINS = listOf(
            "https://dudoong.com",
            "https://internal-admin.dudoong.com",
        )
        val STAGING_ORIGINS = listOf(
            "https://staging.dudoong.com",
            "http://localhost:3000",
            "http://localhost:5173",
        )
    }
}
