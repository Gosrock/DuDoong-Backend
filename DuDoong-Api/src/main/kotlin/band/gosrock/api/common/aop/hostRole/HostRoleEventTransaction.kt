package band.gosrock.api.common.aop.hostRole

import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import org.aspectj.lang.ProceedingJoinPoint
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** 호스트 정보를 트랜잭션 안에서 조회하기 위해서 만든 클래스입니다. 트랜잭션 내에서 캐시 할수 있으면 좋으니 이렇게 만들었습니다. - 이찬진 */
@Component
internal class HostRoleEventTransaction(
    private val superAdminBypass: SuperAdminBypass,
    private val eventAdaptor: EventAdaptor,
    private val hostAdaptor: HostAdaptor
) : HostRoleCallTransaction {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    override fun proceed(userId: Long, eventId: Long, role: HostQualification, joinPoint: ProceedingJoinPoint): Any? {
        validRole(userId, eventId, role, joinPoint.signature.toShortString())
        return joinPoint.proceed()
    }

    private fun validRole(userId: Long, eventId: Long, role: HostQualification, action: String) {
        // SUPER_ADMIN 예외는 SuperAdminBypass 한 곳에서 판정하고 감사 로그를 남긴다 (#763)
        if (superAdminBypass.bypass(userId, action, "EVENT", eventId)) return
        val event = eventAdaptor.findById(eventId)
        val host = hostAdaptor.findById(event.hostId!!)
        role.validQualification(userId, host)
        log.info("[AUTH] Host role verified - userId={}, eventId={}, required={}", userId, eventId, role)
    }
}
