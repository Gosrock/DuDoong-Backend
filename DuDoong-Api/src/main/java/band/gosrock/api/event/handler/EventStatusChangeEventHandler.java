package band.gosrock.api.event.handler;


import band.gosrock.domain.common.alarm.EventSlackAlarm;
import band.gosrock.domain.common.events.event.EventStatusChangeEvent;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
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
public class EventStatusChangeEventHandler {
    private final HostAdaptor hostAdaptor;
    private final EventAdaptor eventAdaptor;
    private final SlackMessageProvider slackMessageProvider;

    @Async
    @TransactionalEventListener(
            classes = EventStatusChangeEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handle(EventStatusChangeEvent eventStatusChangeEvent) {
        final Host host = hostAdaptor.findById(eventStatusChangeEvent.getHostId());
        final Event event = eventAdaptor.findById(eventStatusChangeEvent.getEventId());
        final String message = EventSlackAlarm.changeStatusOf(event);

        slackMessageProvider.sendMessage(host.getSlackUrl(), message);
    }
}
