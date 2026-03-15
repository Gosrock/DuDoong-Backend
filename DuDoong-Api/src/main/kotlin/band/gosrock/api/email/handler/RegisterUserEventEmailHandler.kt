package band.gosrock.api.email.handler

import band.gosrock.api.email.service.SendRegisterEmailService
import band.gosrock.domain.common.events.user.UserRegisterEvent
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

private val log = LoggerFactory.getLogger(RegisterUserEventEmailHandler::class.java)

@Component
class RegisterUserEventEmailHandler(
    private val userAdaptor: UserAdaptor,
    private val sendRegisterEmailService: SendRegisterEmailService,
) {
    @Async
    @TransactionalEventListener(
        classes = [UserRegisterEvent::class],
        phase = TransactionPhase.AFTER_COMMIT,
    )
    fun handleRegisterUserEvent(userRegisterEvent: UserRegisterEvent) {
        val userId = userRegisterEvent.userId
        val user = userAdaptor.queryUser(userId)
        log.info("${userId}유저 등록")
        sendRegisterEmailService.execute(user.toEmailUserInfo())
    }
}
