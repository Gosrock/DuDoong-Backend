package band.gosrock.api.slack.handler.host

import band.gosrock.domain.common.alarm.HostSlackAlarm
import band.gosrock.domain.common.events.host.HostRegisterSlackEvent
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.infrastructure.config.slack.SlackMessageProvider
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = LoggerFactory.getLogger(HostRegisterSlackEventHandler::class.java)

@Component
class HostRegisterSlackEventHandler(
    private val hostAdaptor: HostAdaptor,
    private val slackMessageProvider: SlackMessageProvider,
) {
    @Async
    @TransactionalEventListener(
        classes = [HostRegisterSlackEvent::class],
        phase = TransactionPhase.AFTER_COMMIT,
    )
    fun handle(hostRegisterSlackEvent: HostRegisterSlackEvent) {
        val host = hostAdaptor.findById(hostRegisterSlackEvent.hostId!!)
        val message = HostSlackAlarm.slackRegistrationOf(host)
        slackMessageProvider.sendMessage(host.slackUrl, message)
    }
}
