package band.gosrock.api.slack.handler.host

import band.gosrock.api.email.service.HostMasterChangeEmailService
import band.gosrock.api.email.service.HostUserRoleChangeEmailService
import band.gosrock.domain.common.alarm.HostSlackAlarm
import band.gosrock.domain.common.events.host.HostUserRoleChangeEvent
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.infrastructure.config.slack.SlackMessageProvider
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = LoggerFactory.getLogger(HostUserRoleChangeEventHandler::class.java)

@Component
class HostUserRoleChangeEventHandler(
    private val userAdaptor: UserAdaptor,
    private val hostAdaptor: HostAdaptor,
    private val hostMasterChangeEmailService: HostMasterChangeEmailService,
    private val hostUserRoleChangeEmailService: HostUserRoleChangeEmailService,
    private val slackMessageProvider: SlackMessageProvider,
) {
    @Async
    @TransactionalEventListener(
        classes = [HostUserRoleChangeEvent::class],
        phase = TransactionPhase.AFTER_COMMIT,
    )
    fun handle(hostUserRoleChangeEvent: HostUserRoleChangeEvent) {
        val userId = hostUserRoleChangeEvent.userId!!
        val user = userAdaptor.queryUser(userId)
        val host = hostAdaptor.findById(hostUserRoleChangeEvent.hostId!!)
        val role = hostUserRoleChangeEvent.role!!
        val hostName = hostUserRoleChangeEvent.hostName!!
        val message = HostSlackAlarm.changeMasterOf(host, user)

        // role == master 이면 전체에게 추가 알림 + 이메일
        if (role == HostRole.MASTER) {
            // todo :: host users foreach
            // todo :: 마스터 유저 권한 부여 api
            hostMasterChangeEmailService.execute(user.toEmailUserInfo(), hostName, role)
            slackMessageProvider.sendMessage(host.slackUrl, message)
        } else {
            hostUserRoleChangeEmailService.execute(user.toEmailUserInfo(), hostName, role)
        }
    }
}
