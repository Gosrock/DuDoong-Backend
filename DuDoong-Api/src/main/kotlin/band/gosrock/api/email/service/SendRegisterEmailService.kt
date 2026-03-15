package band.gosrock.api.email.service

import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo
import band.gosrock.infrastructure.config.ses.AwsSesUtils
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context

@Service
class SendRegisterEmailService(
    private val awsSesUtils: AwsSesUtils,
) {
    fun execute(emailUserInfo: EmailUserInfo) {
        val context = Context()
        context.setVariable("username", emailUserInfo.name)
        awsSesUtils.singleEmailRequest(emailUserInfo, "두둥에 회원가입하신것을 축하드립니다!", "signUp", context)
    }
}
