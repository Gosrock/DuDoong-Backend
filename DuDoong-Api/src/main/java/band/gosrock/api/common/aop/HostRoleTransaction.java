package band.gosrock.api.common.aop;


import band.gosrock.api.common.UserUtils;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class HostRoleTransaction {

    private final UserUtils userUtils;
    private final EventAdaptor eventAdaptor;

    private final HostAdaptor hostAdaptor;

    @Transactional(readOnly = true)
    public Object proceed(Long eventId, HostQualification role, final ProceedingJoinPoint joinPoint)
            throws Throwable {
        Long currentUserId = userUtils.getCurrentUserId();
        Event event = eventAdaptor.findById(eventId);
        Host host = hostAdaptor.findById(event.getHostId());
        role.validQualification(currentUserId, host);

        return joinPoint.proceed();
    }
}
