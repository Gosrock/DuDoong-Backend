package band.gosrock.dto

import java.time.LocalDateTime

data class SettlementPDFDto(
    val eventTitle: String,
    val hostName: String,
    val settlementAt: LocalDateTime,
    val dudoongTicketAmount: String,
    val pgTicketAmount: String,
    val totalAmount: String,
    val dudoongFee: String,
    val pgFee: String,
    val totalFee: String,
    val totalFeeVat: String,
    val totalSettlement: String,
    val now: LocalDateTime,
)
