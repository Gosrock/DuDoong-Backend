package band.gosrock.api.event.handler;


import band.gosrock.domain.common.alarm.EventSlackAlarm;
import band.gosrock.domain.common.events.event.EventCreationEvent;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
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
public class EventCreationEventHandler {
    private final HostAdaptor hostAdaptor;
    private final EventAdaptor eventAdaptor;
    private final SlackMessageProvider slackMessageProvider;

    @Async
    @TransactionalEventListener(
            classes = EventCreationEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handle(EventCreationEvent eventCreationEvent) {
        final Host host = hostAdaptor.findById(eventCreationEvent.getHostId());
        final String message = EventSlackAlarm.creationOf(eventCreationEvent.getEventName());

        slackMessageProvider.sendMessage(host.getSlackUrl(), message);
    }
}
