package band.gosrock.api.email.service

import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo
import band.gosrock.infrastructure.config.ses.AwsSesUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context

private val log = LoggerFactory.getLogger(HostUserDisabledEmailService::class.java)

@Service
class HostUserDisabledEmailService(
    private val awsSesUtils: AwsSesUtils,
) {
    fun execute(userInfo: EmailUserInfo, hostName: String) {
        val context = Context()
        context.setVariable("userInfo", userInfo)
        context.setVariable("hostName", hostName)
        log.info("$hostName 에서 추방당함, $userInfo")
        // todo : 호스트에서 추방 템플릿 추가
    }
}
