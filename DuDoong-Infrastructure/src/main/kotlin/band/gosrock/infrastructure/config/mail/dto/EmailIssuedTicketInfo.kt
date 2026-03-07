package band.gosrock.infrastructure.config.mail.dto

import java.time.LocalDateTime

data class EmailIssuedTicketInfo(
    val issuedTicketNo: String,
    val ticketName: String,
    val createdAt: LocalDateTime,
    val issuedTicketStatus: String,
    val money: String,
)
