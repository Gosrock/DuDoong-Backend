package band.gosrock.api.slack.handler.event

import band.gosrock.domain.common.alarm.EventSlackAlarm
import band.gosrock.domain.common.events.event.EventCreationEvent
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.infrastructure.config.slack.SlackMessageProvider
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = LoggerFactory.getLogger(EventCreationEventHandler::class.java)

@Component
class EventCreationEventHandler(
    private val hostAdaptor: HostAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val slackMessageProvider: SlackMessageProvider,
) {
    @Async
    @TransactionalEventListener(
        classes = [EventCreationEvent::class],
        phase = TransactionPhase.AFTER_COMMIT,
    )
    fun handle(eventCreationEvent: EventCreationEvent) {
        val host = hostAdaptor.findById(eventCreationEvent.hostId!!)
        val message = EventSlackAlarm.creationOf(eventCreationEvent.eventName!!)
        slackMessageProvider.sendMessage(host.slackUrl, message)
    }
}
