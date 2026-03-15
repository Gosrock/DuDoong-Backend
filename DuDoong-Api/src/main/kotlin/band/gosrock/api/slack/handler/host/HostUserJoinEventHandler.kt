package band.gosrock.api.slack.handler.host

import band.gosrock.domain.common.alarm.HostSlackAlarm
import band.gosrock.domain.common.events.host.HostUserJoinEvent
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.infrastructure.config.slack.SlackMessageProvider
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = LoggerFactory.getLogger(HostUserJoinEventHandler::class.java)

@Component
class HostUserJoinEventHandler(
    private val userAdaptor: UserAdaptor,
    private val hostAdaptor: HostAdaptor,
    private val slackMessageProvider: SlackMessageProvider,
) {
    @Async
    @TransactionalEventListener(
        classes = [HostUserJoinEvent::class],
        phase = TransactionPhase.AFTER_COMMIT,
    )
    fun handle(hostUserJoinEvent: HostUserJoinEvent) {
        val user = userAdaptor.queryUser(hostUserJoinEvent.userId!!)
        val host = hostAdaptor.findById(hostUserJoinEvent.hostId!!)
        val message = HostSlackAlarm.joinOf(host, user)
        slackMessageProvider.sendMessage(host.slackUrl, message)
    }
}
