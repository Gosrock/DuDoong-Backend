package band.gosrock.api.email.service

import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo
import band.gosrock.infrastructure.config.ses.AwsSesUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context

private val log = LoggerFactory.getLogger(HostUserRoleChangeEmailService::class.java)

@Service
class HostUserRoleChangeEmailService(
    private val awsSesUtils: AwsSesUtils,
) {
    fun execute(userInfo: EmailUserInfo, hostName: String, hostRole: HostRole) {
        val context = Context()
        context.setVariable("userInfo", userInfo)
        context.setVariable("hostName", hostName)
        context.setVariable("role", hostRole.value)
        log.info("$hostName 의 역할 변경 알림. $userInfo")
        // todo : 당신의 역할이 변경되었음을 알리는 템플릿 추가
    }
}
