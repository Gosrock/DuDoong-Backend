package band.gosrock.api.alimTalk.handler

import band.gosrock.api.alimTalk.service.SendRegisterAlimTalkService
import band.gosrock.domain.common.events.user.UserRegisterEvent
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class RegisterUserEventAlimTalkHandler(
    private val userAdaptor: UserAdaptor,
    private val sendRegisterAlimTalkService: SendRegisterAlimTalkService,
) {
    private val log = LoggerFactory.getLogger(RegisterUserEventAlimTalkHandler::class.java)

    @Async
    @TransactionalEventListener(classes = [UserRegisterEvent::class], phase = TransactionPhase.AFTER_COMMIT)
    fun handleRegisterUserEvent(userRegisterEvent: UserRegisterEvent) {
        val userId = userRegisterEvent.userId
        try {
            val user = userAdaptor.queryUser(userId)
            log.info("${userId}유저 등록")
            val userInfo = user.toAlimTalkUserInfo()
            sendRegisterAlimTalkService.execute(userInfo.userName, userInfo.phoneNum)
        } catch (e: Exception) {
            log.warn("유저 등록 알림톡 전송 실패 (userId=$userId): ${e.message}")
        }
    }
}
