package band.gosrock.api.slack.handler.host

import band.gosrock.api.email.service.HostUserDisabledEmailService
import band.gosrock.domain.common.alarm.HostSlackAlarm
import band.gosrock.domain.common.events.host.HostUserDisabledEvent
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.infrastructure.config.slack.SlackMessageProvider
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = LoggerFactory.getLogger(HostUserDisabledEventHandler::class.java)

@Component
class HostUserDisabledEventHandler(
    private val userAdaptor: UserAdaptor,
    private val hostAdaptor: HostAdaptor,
    private val hostUserDisabledEmailService: HostUserDisabledEmailService,
    private val slackMessageProvider: SlackMessageProvider,
) {
    @Async
    @TransactionalEventListener(
        classes = [HostUserDisabledEvent::class],
        phase = TransactionPhase.AFTER_COMMIT,
    )
    fun handle(hostUserDisabledEvent: HostUserDisabledEvent) {
        val userId = hostUserDisabledEvent.userId!!
        val user = userAdaptor.queryUser(userId)
        val host = hostAdaptor.findById(hostUserDisabledEvent.hostId!!)
        val hostName = hostUserDisabledEvent.hostName!!
        val message = HostSlackAlarm.disabledOf(user)
        hostUserDisabledEmailService.execute(user.toEmailUserInfo(), hostName)
        slackMessageProvider.sendMessage(host.slackUrl, message)
    }
}
