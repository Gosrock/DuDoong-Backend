package band.gosrock.infrastructure.config.alilmTalk.dto

import java.time.LocalDateTime

data class AlimTalkOrderInfo(
    val name: String,
    val quantity: Long,
    val money: String,
    val createAt: LocalDateTime,
)
