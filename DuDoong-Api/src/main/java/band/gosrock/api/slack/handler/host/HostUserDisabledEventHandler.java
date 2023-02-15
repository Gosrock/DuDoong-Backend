package band.gosrock.api.slack.handler.host;


import band.gosrock.api.email.service.HostUserDisabledEmailService;
import band.gosrock.domain.common.alarm.HostSlackAlarm;
import band.gosrock.domain.common.events.host.HostUserDisabledEvent;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
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
public class HostUserDisabledEventHandler {
    private final UserAdaptor userAdaptor;
    private final HostAdaptor hostAdaptor;
    private final HostUserDisabledEmailService hostUserDisabledEmailService;
    private final SlackMessageProvider slackMessageProvider;

    @Async
    @TransactionalEventListener(
            classes = HostUserDisabledEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handle(HostUserDisabledEvent hostUserDisabledEvent) {
        final Long userId = hostUserDisabledEvent.getUserId();
        final User user = userAdaptor.queryUser(userId);
        final Host host = hostAdaptor.findById(hostUserDisabledEvent.getHostId());
        final String hostName = hostUserDisabledEvent.getHostName();
        final String message = HostSlackAlarm.disabledOf(user);

        hostUserDisabledEmailService.execute(user.toEmailUserInfo(), hostName);
        slackMessageProvider.sendMessage(host.getSlackUrl(), message);
    }
}
