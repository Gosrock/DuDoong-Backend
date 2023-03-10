package band.gosrock.api.common.aop.hostPartner;


import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
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
class HostPartnerEventTransaction implements HostPartnerCallTransaction {

    private final EventAdaptor eventAdaptor;
    private final HostAdaptor hostAdaptor;

    @Transactional(readOnly = true)
    public Object proceed(Long eventId, final ProceedingJoinPoint joinPoint) throws Throwable {
        Event event = eventAdaptor.findById(eventId);
        Host host = hostAdaptor.findById(event.getHostId());
        host.validatePartnerHost();
        return joinPoint.proceed();
    }
}
