package band.gosrock.api.alimTalk.dto

import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkEventInfo
import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkOrderInfo
import band.gosrock.infrastructure.config.alilmTalk.dto.AlimTalkUserInfo

data class OrderAlimTalkDto(
    val userInfo: AlimTalkUserInfo,
    val orderInfo: AlimTalkOrderInfo,
    val eventInfo: AlimTalkEventInfo,
)
