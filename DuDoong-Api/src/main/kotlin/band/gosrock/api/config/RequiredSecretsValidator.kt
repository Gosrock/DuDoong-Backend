package band.gosrock.api.config

import band.gosrock.common.properties.JwtProperties
import band.gosrock.common.properties.TossPaymentsProperties
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * 비밀값 기동 검사 (#764). yml 에 기본값이 없어서 값이 없으면 `${JWT_SECRET_KEY}` 같은 자리표시자 문자열이 그대로 들어온다 — 이 경우 기동을 멈춘다.
 * - JWT 키: 32바이트(HS256) 이상. 운영·스테이징에서는 로컬·테스트용 키(접두사 [TEST_JWT_KEY_PREFIX])도 거부
 * - 토스 시크릿 키·MID: 비어 있거나 자리표시자면 거부
 * AWS 키는 `@Value` 로 주입되어 값이 없으면 스프링이 기동 단계에서 실패시킨다
 */
@Component
class RequiredSecretsValidator(
    private val environment: Environment,
    private val jwtProperties: JwtProperties,
    private val tossPaymentsProperties: TossPaymentsProperties,
) : InitializingBean {

    override fun afterPropertiesSet() {
        val errors = buildList {
            val jwtKey = jwtProperties.secretKey
            if (isMissing(jwtKey)) {
                add("JWT_SECRET_KEY 가 없습니다")
            } else {
                if (jwtKey.toByteArray(Charsets.UTF_8).size < MIN_JWT_KEY_BYTES) add("JWT_SECRET_KEY 가 ${MIN_JWT_KEY_BYTES}바이트보다 짧습니다")
                if (isDeployed() && jwtKey.startsWith(TEST_JWT_KEY_PREFIX)) add("운영·스테이징에서 테스트용 JWT_SECRET_KEY 를 쓸 수 없습니다")
            }
            if (isMissing(tossPaymentsProperties.secretKey)) add("TOSS_PAYMENTS_KEY 가 없습니다")
            if (isMissing(tossPaymentsProperties.mid)) add("TOSS_MID 가 없습니다")
        }
        check(errors.isEmpty()) { "필수 비밀값 설정 오류: ${errors.joinToString(", ")}" }
    }

    private fun isMissing(value: String?): Boolean = value.isNullOrBlank() || value.contains("\${")

    private fun isDeployed(): Boolean = environment.activeProfiles.any { it in DEPLOYED_PROFILES }

    companion object {
        const val MIN_JWT_KEY_BYTES = 32
        const val TEST_JWT_KEY_PREFIX = "testkey"
        private val DEPLOYED_PROFILES = setOf("prod", "staging")
    }
}
