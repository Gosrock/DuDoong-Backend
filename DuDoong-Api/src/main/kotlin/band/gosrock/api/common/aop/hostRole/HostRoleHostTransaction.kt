package band.gosrock.api.common.aop.hostRole

import band.gosrock.api.common.UserUtils
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import org.aspectj.lang.ProceedingJoinPoint
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** 호스트 정보를 트랜잭션 안에서 조회하기 위해서 만든 클래스입니다. 트랜잭션 내에서 캐시 할수 있으면 좋으니 이렇게 만들었습니다. - 이찬진 */
@Component
internal class HostRoleHostTransaction(
    private val userUtils: UserUtils,
    private val hostAdaptor: HostAdaptor
) : HostRoleCallTransaction {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    override fun proceed(hostId: Long, role: HostQualification, joinPoint: ProceedingJoinPoint): Any? {
        validRole(hostId, role)
        return joinPoint.proceed()
    }

    private fun validRole(hostId: Long, role: HostQualification) {
        val currentUserId = userUtils.getCurrentUserId()
        val host = hostAdaptor.findById(hostId)
        role.validQualification(currentUserId, host)
    }
}
