package band.gosrock.api.email.service

import band.gosrock.api.email.dto.IssuedTicketMailDTO
import band.gosrock.infrastructure.config.ses.AwsSesUtils
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context

@Service
class EntranceIssuedTicketEmailService(
    private val awsSesUtils: AwsSesUtils,
) {
    fun execute(issuedTicketMailDTO: IssuedTicketMailDTO) {
        val subject = "[두둥] 입장 확인 알림드립니다."
        val context = Context()
        val userInfo = issuedTicketMailDTO.userInfo

        context.setVariable("userInfo", userInfo)
        context.setVariable("issuedTicketInfo", issuedTicketMailDTO.issuedTicketInfo)
        context.setVariable("eventInfo", issuedTicketMailDTO.eventInfo)

        awsSesUtils.singleEmailRequest(userInfo, subject, "entranceIssuedTicket", context)
    }
}
