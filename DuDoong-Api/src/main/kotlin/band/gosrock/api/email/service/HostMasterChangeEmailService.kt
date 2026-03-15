package band.gosrock.api.email.service

import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo
import band.gosrock.infrastructure.config.ses.AwsSesUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context

private val log = LoggerFactory.getLogger(HostMasterChangeEmailService::class.java)

@Service
class HostMasterChangeEmailService(
    private val awsSesUtils: AwsSesUtils,
) {
    fun execute(userInfo: EmailUserInfo, hostName: String, hostRole: HostRole) {
        val context = Context()
        context.setVariable("userInfo", userInfo)
        context.setVariable("hostName", hostName)
        context.setVariable("role", hostRole.value)
        log.info("$hostName 에서 마스터 변경 알림, $userInfo")
        // todo : 마스터 변경 템플릿 추가
    }
}
