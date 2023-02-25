package band.gosrock.common.aop;


import band.gosrock.common.exception.DuDoongDynamicException;
import band.gosrock.common.helper.SpringEnvironmentHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiBlockingAspect {

    private final SpringEnvironmentHelper springEnvironmentHelper;

    @Around("@annotation(band.gosrock.common.annotation.DevelopOnlyApi)")
    public Object checkApiAcceptingCondition(ProceedingJoinPoint joinPoint) throws Throwable {
        if (springEnvironmentHelper.isProdProfile()) {
            throw new DuDoongDynamicException(405, "Blocked Api", "not working api in production");
        }
        return joinPoint.proceed();
    }
}
