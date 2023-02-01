package band.gosrock.api.common.aop.hostRole;


import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * 호스트 관리자 인가를 위한 aop 입니다 메소드 레벨에서 작동하며 권한 정보를 어노테이션으로 받고 eventId를 인자에서 찾아와 호스트 정보를 불러온뒤 권한 검증을 합니다.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("${ableHostRoleAop:true}")
public class HostRoleAop {
    private final HostCallTransactionFactory hostCallTransactionFactory;

    /**
     * master 호스트의 마스터 manager 호스트의 수정,조회 ( 호스트유저도메인의 슈퍼 호스트 ) guest 호스트의 조회권한 (호스트유저도메인의 호스트 )
     *
     * @see band.gosrock.domain.domains.host.domain.HostRole
     */
    @Around("@annotation(band.gosrock.api.common.aop.hostRole.HostRolesAllowed)")
    public Object aop(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        HostRolesAllowed annotation = method.getAnnotation(HostRolesAllowed.class);
        HostQualification hostQualification = annotation.role();
        // 제공된 호스트의 role 이 정의된 세개의 롤과 같은지 확인한다.
        // 없으면 IllegalArgumentException 발생
        FindHostFrom findHostFrom = annotation.findhostFrom();
        String identifier = findHostFrom.getIdentifier();

        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        Long id = getId(parameterNames, args, identifier);

        return hostCallTransactionFactory
                .getCallTransaction(findHostFrom)
                .proceed(id, hostQualification, joinPoint);
    }

    public Long getId(String[] parameterNames, Object[] args, String paramName) {
        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals(paramName)) {
                // 롱타입이라 가정. 안되면 classCastException 터트림
                return (Long) args[i];
            }
        }
        throw new IllegalArgumentException();
    }
}
