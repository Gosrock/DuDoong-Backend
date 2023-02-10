package band.gosrock.api.host.handler;


import band.gosrock.domain.common.alarm.HostSlackAlarm;
import band.gosrock.domain.common.events.host.HostRegisterSlackEvent;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
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
public class HostRegisterSlackEventHandler {
    private final HostAdaptor hostAdaptor;
    private final SlackMessageProvider slackMessageProvider;

    @Async
    @TransactionalEventListener(
            classes = HostRegisterSlackEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handle(HostRegisterSlackEvent hostRegisterSlackEvent) {
        final Host host = hostAdaptor.findById(hostRegisterSlackEvent.getHostId());
        final String message = HostSlackAlarm.slackRegistrationOf(host);

        slackMessageProvider.sendMessage(host.getSlackUrl(), message);
    }
}
