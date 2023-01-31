package band.gosrock.api.common.aop;


import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("${ableHostRoleAop:true}")
public class HostRoleAop {
    private final HostRoleTransaction hostRoleTransaction;

    /**
     * master 호스트의 마스터 manager 호스트의 수정,조회 ( 호스트유저도메인의 슈퍼 호스트 ) guest 호스트의 조회권한 (호스트유저도메인의 호스트 )
     *
     * @see band.gosrock.domain.domains.host.domain.HostRole
     */
    @Around("@annotation(band.gosrock.api.common.aop.HostRolesAllowed)")
    public Object aop(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        HostRolesAllowed annotation = method.getAnnotation(HostRolesAllowed.class);
        String role = annotation.value();

        // 제공된 호스트의 role 이 정의된 세개의 롤과 같은지 확인한다.
        // 없으면 IllegalArgumentException 발생
        HostQualification hostQualification = HostQualification.valueOf(role);

        String identifier = annotation.eventIdIdentifier();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        // 이벤트 아이디를 인자에서 얻어온다.
        Long eventId = getEventId(parameterNames, args, identifier);

        return hostRoleTransaction.proceed(eventId, hostQualification, joinPoint);
    }

    public Long getEventId(String[] parameterNames, Object[] args, String paramName) {
        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(paramName)) {
                // 롱타입이라 가정. 안되면 classCastException 터트림
                return (Long) args[i];
            }
        }
        throw new IllegalArgumentException();
    }
}
