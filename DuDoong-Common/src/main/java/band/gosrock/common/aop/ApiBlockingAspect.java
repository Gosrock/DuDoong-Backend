package band.gosrock.common.aop;


import band.gosrock.common.exception.DuDoongDynamicException;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ApiBlockingAspect {

    private final Environment env;

    @Around("@annotation(band.gosrock.common.annotation.DevelopOnlyApi)")
    public Object checkApiAcceptingCondition(ProceedingJoinPoint joinPoint) throws Throwable {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.stream(activeProfiles).toList().contains("prod")) {
            throw new DuDoongDynamicException(405, "Blocked Api", "not working api in production");
        }
        return joinPoint.proceed();
    }
}
