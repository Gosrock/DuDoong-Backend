package band.gosrock.api.common.aop.hostPartner

import org.aspectj.lang.ProceedingJoinPoint

internal interface HostPartnerCallTransaction {
    @Throws(Throwable::class)
    fun proceed(id: Long, joinPoint: ProceedingJoinPoint): Any?
}
