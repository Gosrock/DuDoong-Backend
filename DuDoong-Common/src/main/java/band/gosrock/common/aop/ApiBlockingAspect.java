package band.gosrock.common.aop;


import band.gosrock.common.exception.DuDoongDynamicException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ApiBlockingAspect {

    @Value("${spring.config.activate.on-profile}")
    private String profile;

    @Around("@annotation(band.gosrock.common.annotation.DevelopOnlyApi)")
    public Object checkApiAcceptingCondition(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("[ABA::checkApiAcceptingCondition] environment={}", profile);
        if (profile.equals("prod")) {
            throw new DuDoongDynamicException(405, "Blocked Api", "not working api in production");
        }
        return joinPoint.proceed();
    }
}
