package band.gosrock.api.common.aop.hostPartner;


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
class HostPartnerHostTransaction implements HostPartnerCallTransaction {
    private final HostAdaptor hostAdaptor;

    @Transactional(readOnly = true)
    public Object proceed(Long hostId, final ProceedingJoinPoint joinPoint) throws Throwable {
        Host host = hostAdaptor.findById(hostId);
        host.validatePartnerHost();
        return joinPoint.proceed();
    }
}
