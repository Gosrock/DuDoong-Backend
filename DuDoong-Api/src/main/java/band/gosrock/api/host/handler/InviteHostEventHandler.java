package band.gosrock.api.host.handler;


import band.gosrock.api.email.service.HostInviteEmailService;
import band.gosrock.domain.common.events.host.InviteHostEvent;
import band.gosrock.domain.domains.host.domain.HostRole;
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
public class InviteHostEventHandler {
    private final UserAdaptor userAdaptor;
    private final HostInviteEmailService inviteEmailService;

    @Async
    @TransactionalEventListener(
            classes = InviteHostEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleRegisterUserEvent(InviteHostEvent inviteHostEvent) {
        final Long userId = inviteHostEvent.getUserId();
        final User user = userAdaptor.queryUser(userId);
        final HostRole role = inviteHostEvent.getRole();
        final String hostName = inviteHostEvent.getHostProfileVo().getName();

        inviteEmailService.execute(user.toEmailUserInfo(), hostName, role);
    }
}
