package band.gosrock.api.common.aop.hostRole

import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 호스트 API 의 SUPER_ADMIN 예외 기준 (#763). 호스트 멤버 권한 검사(G+/M+/MS, 준비중 공연 조회)를 SUPER_ADMIN 은 건너뛴다.
 * v1·v2 의 모든 예외 지점이 이 클래스로 판정하고, 예외를 쓴 때마다 감사 로그를 남긴다.
 *
 * 감사 로그 형식(한 줄): `[AUDIT] SUPER_ADMIN_BYPASS userId={} action={} target={}:{}` — 로거 이름 `AUDIT.SuperAdminBypass`
 * 운영 작업은 /internal-api 로 모으는 것이 원칙이고, 이 예외는 운영 영향 때문에 남겨 둔다.
 */
@Component
class SuperAdminBypass(
    private val userAdaptor: UserAdaptor,
) {

    /** SUPER_ADMIN 이면 감사 로그를 남기고 true. 비로그인(userId 0)을 받는 곳은 부르기 전에 걸러야 한다 (없는 유저 조회) */
    fun bypass(userId: Long, action: String, targetType: String, targetId: Any?): Boolean {
        if (userAdaptor.queryUser(userId).accountRole != AccountRole.SUPER_ADMIN) return false
        auditLog.info("[AUDIT] SUPER_ADMIN_BYPASS userId={} action={} target={}:{}", userId, action, targetType, targetId)
        return true
    }

    companion object {
        private val auditLog = LoggerFactory.getLogger("AUDIT.SuperAdminBypass")
    }
}
