package band.gosrock.api.slack.handler.event;


import band.gosrock.domain.common.alarm.EventSlackAlarm;
import band.gosrock.domain.common.events.event.EventContentChangeEvent;
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
public class EventContentChangeEventHandler {
    private final HostAdaptor hostAdaptor;
    private final EventAdaptor eventAdaptor;
    private final SlackMessageProvider slackMessageProvider;

    @Async
    @TransactionalEventListener(
            classes = EventContentChangeEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handle(EventContentChangeEvent eventContentChangeEvent) {
        final Host host = hostAdaptor.findById(eventContentChangeEvent.getHostId());
        final Event event = eventAdaptor.findById(eventContentChangeEvent.getEventId());
        final String message = EventSlackAlarm.changeContentOf(event);

        slackMessageProvider.sendMessage(host.getSlackUrl(), message);
    }
}
