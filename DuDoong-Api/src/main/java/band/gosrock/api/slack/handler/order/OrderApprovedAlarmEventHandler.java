package band.gosrock.api.slack.handler.order;


import band.gosrock.api.email.service.HostUserInvitationEmailService;
import band.gosrock.domain.common.events.host.HostUserInvitationEvent;
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderApprovedAlarmEventHandler {
    private final UserAdaptor userAdaptor;
    private final HostUserInvitationEmailService invitationEmailService;

    @Async
    @TransactionalEventListener(
            classes = HostUserInvitationEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true)
    public void handle(HostUserInvitationEvent hostUserInvitationEvent) {
        final Long userId = hostUserInvitationEvent.getUserId();
        final User user = userAdaptor.queryUser(userId);
        final HostRole role = hostUserInvitationEvent.getRole();
        final String hostName = hostUserInvitationEvent.getHostProfileVo().getName();

        invitationEmailService.execute(user.toEmailUserInfo(), hostName, role);
    }
}
