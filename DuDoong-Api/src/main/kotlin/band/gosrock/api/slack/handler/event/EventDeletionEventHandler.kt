package band.gosrock.api.slack.handler.event

import band.gosrock.domain.common.alarm.EventSlackAlarm
import band.gosrock.domain.common.events.event.EventDeletionEvent
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.infrastructure.config.slack.SlackMessageProvider
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = LoggerFactory.getLogger(EventDeletionEventHandler::class.java)

@Component
class EventDeletionEventHandler(
    private val hostAdaptor: HostAdaptor,
    private val slackMessageProvider: SlackMessageProvider,
) {
    @Async
    @TransactionalEventListener(
        classes = [EventDeletionEvent::class],
        phase = TransactionPhase.AFTER_COMMIT,
    )
    fun handle(eventDeletionEvent: EventDeletionEvent) {
        val host = hostAdaptor.findById(eventDeletionEvent.hostId)
        val eventName = eventDeletionEvent.eventName
        val message = EventSlackAlarm.deletionOf(eventName)
        slackMessageProvider.sendMessage(host.slackUrl, message)
    }
}
