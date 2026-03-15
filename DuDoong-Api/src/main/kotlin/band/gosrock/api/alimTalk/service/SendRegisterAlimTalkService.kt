package band.gosrock.api.alimTalk.service

import band.gosrock.domain.common.alarm.UserKakaoTalkAlarm
import band.gosrock.infrastructure.config.alilmTalk.NcpHelper
import org.springframework.stereotype.Service

@Service
class SendRegisterAlimTalkService(
    private val ncpHelper: NcpHelper,
) {
    fun execute(userName: String, to: String) {
        val content = UserKakaoTalkAlarm.creationOf(userName)
        ncpHelper.sendButtonNcpAlimTalk(to, UserKakaoTalkAlarm.creationTemplateCode(), content)
    }
}
