package band.gosrock.api.email.dto

import band.gosrock.infrastructure.config.mail.dto.EmailEventInfo
import band.gosrock.infrastructure.config.mail.dto.EmailOrderInfo
import band.gosrock.infrastructure.config.mail.dto.EmailUserInfo

data class OrderMailDto(
    val userInfo: EmailUserInfo,
    val orderInfo: EmailOrderInfo,
    val eventInfo: EmailEventInfo,
)
