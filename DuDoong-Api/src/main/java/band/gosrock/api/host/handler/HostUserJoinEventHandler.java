package band.gosrock.api.host.handler;


import band.gosrock.domain.common.events.host.HostUserJoinEvent;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class HostUserJoinEventHandler {
    private final UserAdaptor userAdaptor;
    private final HostAdaptor hostAdaptor;

    @Async
    @TransactionalEventListener(
            classes = HostUserJoinEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleHostUserJoinEvent(HostUserJoinEvent hostUserJoinEvent) {
        final User user = userAdaptor.queryUser(hostUserJoinEvent.getUserId());
        final Host host = hostAdaptor.findById(hostUserJoinEvent.getHostId());

        // todo :: call slack alarm sender
        // host.getHostUsers().forEach();
    }
}
