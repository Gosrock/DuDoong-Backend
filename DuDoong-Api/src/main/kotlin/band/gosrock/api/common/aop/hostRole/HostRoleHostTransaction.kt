package band.gosrock.api.common.aop.hostRole

import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import org.aspectj.lang.ProceedingJoinPoint
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** 호스트 정보를 트랜잭션 안에서 조회하기 위해서 만든 클래스입니다. 트랜잭션 내에서 캐시 할수 있으면 좋으니 이렇게 만들었습니다. - 이찬진 */
@Component
internal class HostRoleHostTransaction(
    private val superAdminBypass: SuperAdminBypass,
    private val hostAdaptor: HostAdaptor
) : HostRoleCallTransaction {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    override fun proceed(userId: Long, hostId: Long, role: HostQualification, joinPoint: ProceedingJoinPoint): Any? {
        validRole(userId, hostId, role, joinPoint.signature.toShortString())
        return joinPoint.proceed()
    }

    private fun validRole(userId: Long, hostId: Long, role: HostQualification, action: String) {
        // SUPER_ADMIN 예외는 SuperAdminBypass 한 곳에서 판정하고 감사 로그를 남긴다 (#763)
        // 대상 존재 확인은 SUPER_ADMIN 도 건너뛰지 않는다 (없으면 404). 권한 검사만 예외
        val host = hostAdaptor.findById(hostId)
        if (superAdminBypass.bypass(userId, action, "HOST", hostId)) return
        role.validQualification(userId, host)
        log.info("[AUTH] Host role verified - userId={}, hostId={}, required={}", userId, hostId, role)
    }
}
