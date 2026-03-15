package band.gosrock.api.slack.handler.event

import band.gosrock.domain.common.alarm.EventSlackAlarm
import band.gosrock.domain.common.events.event.EventStatusChangeEvent
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.infrastructure.config.slack.SlackMessageProvider
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = LoggerFactory.getLogger(EventStatusChangeEventHandler::class.java)

@Component
class EventStatusChangeEventHandler(
    private val hostAdaptor: HostAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val slackMessageProvider: SlackMessageProvider,
) {
    @Async
    @TransactionalEventListener(
        classes = [EventStatusChangeEvent::class],
        phase = TransactionPhase.AFTER_COMMIT,
    )
    fun handle(eventStatusChangeEvent: EventStatusChangeEvent) {
        val host = hostAdaptor.findById(eventStatusChangeEvent.hostId)
        val event = eventAdaptor.findById(eventStatusChangeEvent.eventId)
        val message = EventSlackAlarm.changeStatusOf(event)
        slackMessageProvider.sendMessage(host.slackUrl, message)
    }
}
