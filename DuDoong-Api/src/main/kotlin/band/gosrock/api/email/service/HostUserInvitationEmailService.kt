package band.gosrock.api.email.service

import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo
import band.gosrock.infrastructure.config.ses.AwsSesUtils
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context

@Service
class HostUserInvitationEmailService(
    private val awsSesUtils: AwsSesUtils,
) {
    fun execute(userInfo: EmailUserInfo, hostName: String, hostRole: HostRole) {
        val context = Context()
        context.setVariable("userInfo", userInfo)
        context.setVariable("hostName", hostName)
        context.setVariable("role", hostRole.value)
        awsSesUtils.singleEmailRequest(
            userInfo,
            "두둥$hostName 호스트 초대 알림 드립니다.",
            "hostInvite",
            context,
        )
    }
}
