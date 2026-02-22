package band.gosrock.common.aop

import band.gosrock.common.exception.DuDoongDynamicException
import band.gosrock.common.helper.SpringEnvironmentHelper
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Aspect
@Component
class ApiBlockingAspect(
    private val springEnvironmentHelper: SpringEnvironmentHelper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Around("@annotation(band.gosrock.common.annotation.DevelopOnlyApi)")
    @Throws(Throwable::class)
    fun checkApiAcceptingCondition(joinPoint: ProceedingJoinPoint): Any? {
        if (springEnvironmentHelper.isProdProfile()) {
            throw DuDoongDynamicException(405, "Blocked Api", "not working api in production")
        }
        return joinPoint.proceed()
    }
}
