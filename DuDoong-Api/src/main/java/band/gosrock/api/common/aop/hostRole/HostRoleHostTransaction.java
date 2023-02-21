package band.gosrock.api.common.aop.hostRole;


import band.gosrock.api.common.UserUtils;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 호스트 정보를 트랜잭션 안에서 조회하기 위해서 만든 클래스입니다. 트랜잭션 내에서 캐시 할수 있으면 좋으니 이렇게 만들었습니다. - 이찬진 */
@Component
@RequiredArgsConstructor
@Slf4j
class HostRoleHostTransaction implements HostRoleCallTransaction {

    private final UserUtils userUtils;
    private final HostAdaptor hostAdaptor;

    @Transactional(readOnly = true)
    public Object proceed(Long hostId, HostQualification role, final ProceedingJoinPoint joinPoint)
            throws Throwable {
        validRole(hostId, role);
        return joinPoint.proceed();
    }

    private void validRole(Long hostId, HostQualification role) {
        Long currentUserId = userUtils.getCurrentUserId();
        Host host = hostAdaptor.findById(hostId);
        role.validQualification(currentUserId, host);
    }
}
