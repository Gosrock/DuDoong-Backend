package band.gosrock.api.slack.handler.event;


import band.gosrock.domain.common.alarm.EventSlackAlarm;
import band.gosrock.domain.common.events.event.EventDeletionEvent;
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
public class EventDeletionEventHandler {
    private final HostAdaptor hostAdaptor;
    private final SlackMessageProvider slackMessageProvider;

    @Async
    @TransactionalEventListener(
            classes = EventDeletionEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handle(EventDeletionEvent eventDeletionEvent) {
        final Host host = hostAdaptor.findById(eventDeletionEvent.getHostId());
        final String eventName = eventDeletionEvent.getEventName();
        final String message = EventSlackAlarm.deletionOf(eventName);
        slackMessageProvider.sendMessage(host.getSlackUrl(), message);
    }
}
