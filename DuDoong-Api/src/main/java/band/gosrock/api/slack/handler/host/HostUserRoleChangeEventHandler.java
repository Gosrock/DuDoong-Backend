package band.gosrock.api.slack.handler.host;


import band.gosrock.api.email.service.HostMasterChangeEmailService;
import band.gosrock.api.email.service.HostUserRoleChangeEmailService;
import band.gosrock.domain.common.alarm.HostSlackAlarm;
import band.gosrock.domain.common.events.host.HostUserRoleChangeEvent;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.domain.HostRole;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.infrastructure.config.slack.SlackMessageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class HostUserRoleChangeEventHandler {
    private final UserAdaptor userAdaptor;
    private final HostAdaptor hostAdaptor;
    private final HostMasterChangeEmailService hostMasterChangeEmailService;
    private final HostUserRoleChangeEmailService hostUserRoleChangeEmailService;
    private final SlackMessageProvider slackMessageProvider;

    @Async
    @TransactionalEventListener(
            classes = HostUserRoleChangeEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handle(HostUserRoleChangeEvent hostUserRoleChangeEvent) {
        final Long userId = hostUserRoleChangeEvent.getUserId();
        final User user = userAdaptor.queryUser(userId);
        final Host host = hostAdaptor.findById(hostUserRoleChangeEvent.getHostId());
        final HostRole role = hostUserRoleChangeEvent.getRole();
        final String hostName = hostUserRoleChangeEvent.getHostName();
        final String message = HostSlackAlarm.changeMasterOf(host, user);

        // role == master 이면 전체에게 추가 알림 + 이메일
        if (role == HostRole.MASTER) {
            // todo :: host users foreach
            // todo :: 마스터 유저 권한 부여 api
            hostMasterChangeEmailService.execute(user.toEmailUserInfo(), hostName, role);
            slackMessageProvider.sendMessage(host.getSlackUrl(), message);
        } else {
            hostUserRoleChangeEmailService.execute(user.toEmailUserInfo(), hostName, role);
        }
    }
}
