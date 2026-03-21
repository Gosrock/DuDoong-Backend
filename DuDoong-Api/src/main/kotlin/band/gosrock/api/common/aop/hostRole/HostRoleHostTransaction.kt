package band.gosrock.api.common.aop.hostRole

import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole
import org.aspectj.lang.ProceedingJoinPoint
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** 호스트 정보를 트랜잭션 안에서 조회하기 위해서 만든 클래스입니다. 트랜잭션 내에서 캐시 할수 있으면 좋으니 이렇게 만들었습니다. - 이찬진 */
@Component
internal class HostRoleHostTransaction(
    private val userAdaptor: UserAdaptor,
    private val hostAdaptor: HostAdaptor
) : HostRoleCallTransaction {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    override fun proceed(userId: Long, hostId: Long, role: HostQualification, joinPoint: ProceedingJoinPoint): Any? {
        validRole(userId, hostId, role)
        return joinPoint.proceed()
    }

    private fun validRole(userId: Long, hostId: Long, role: HostQualification) {
        val user = userAdaptor.queryUser(userId)
        if (user.accountRole == AccountRole.SUPER_ADMIN) {
            log.info("[AUTH] SUPER_ADMIN bypass - userId={}, hostId={}", userId, hostId)
            return
        }
        val host = hostAdaptor.findById(hostId)
        role.validQualification(userId, host)
        log.info("[AUTH] Host role verified - userId={}, hostId={}, required={}", userId, hostId, role)
    }
}
