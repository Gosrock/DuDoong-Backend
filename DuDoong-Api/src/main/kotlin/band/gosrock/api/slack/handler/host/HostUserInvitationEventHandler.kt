package band.gosrock.api.slack.handler.host

import band.gosrock.api.email.service.HostUserInvitationEmailService
import band.gosrock.domain.common.events.host.HostUserInvitationEvent
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = LoggerFactory.getLogger(HostUserInvitationEventHandler::class.java)

@Component
class HostUserInvitationEventHandler(
    private val userAdaptor: UserAdaptor,
    private val invitationEmailService: HostUserInvitationEmailService,
) {
    @Async
    @TransactionalEventListener(
        classes = [HostUserInvitationEvent::class],
        phase = TransactionPhase.AFTER_COMMIT,
    )
    fun handle(hostUserInvitationEvent: HostUserInvitationEvent) {
        val userId = hostUserInvitationEvent.userId!!
        val user = userAdaptor.queryUser(userId)
        val role = hostUserInvitationEvent.role!!
        val hostName = hostUserInvitationEvent.hostProfileVo!!.name!!
        invitationEmailService.execute(user.toEmailUserInfo(), hostName, role)
    }
}
