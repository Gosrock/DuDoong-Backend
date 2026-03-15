package band.gosrock.api.email.dto

import band.gosrock.infrastructure.config.mail.dto.EmailEventInfo
import band.gosrock.infrastructure.config.mail.dto.EmailIssuedTicketInfo
import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo

data class IssuedTicketMailDTO(
    val userInfo: EmailUserInfo,
    val issuedTicketInfo: EmailIssuedTicketInfo,
    val eventInfo: EmailEventInfo,
)
