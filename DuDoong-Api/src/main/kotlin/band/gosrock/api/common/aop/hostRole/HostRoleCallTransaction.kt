package band.gosrock.api.common.aop.hostRole

import org.aspectj.lang.ProceedingJoinPoint

internal interface HostRoleCallTransaction {
    @Throws(Throwable::class)
    fun proceed(userId: Long, id: Long, role: HostQualification, joinPoint: ProceedingJoinPoint): Any?
}
