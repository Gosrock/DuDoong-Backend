package band.gosrock.domain.common.aop.redissonLock

import org.aspectj.lang.ProceedingJoinPoint

interface CallTransaction {
    @Throws(Throwable::class)
    fun proceed(joinPoint: ProceedingJoinPoint): Any?
}
