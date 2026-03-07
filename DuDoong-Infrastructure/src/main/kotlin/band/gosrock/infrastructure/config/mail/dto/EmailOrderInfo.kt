package band.gosrock.infrastructure.config.mail.dto

import java.time.LocalDateTime

data class EmailOrderInfo(
    val name: String,
    val quantity: Long,
    val money: String,
    val createAt: LocalDateTime,
)
