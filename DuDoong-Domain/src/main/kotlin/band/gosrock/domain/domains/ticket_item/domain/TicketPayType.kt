package band.gosrock.domain.domains.ticket_item.domain

import com.fasterxml.jackson.annotation.JsonValue

enum class TicketPayType(
    val value: String,
    @JsonValue val kr: String,
) {
    // 두둥티켓
    DUDOONG_TICKET("DUDOONG_TICKET", "두둥티켓"),
    // 무료티켓
    FREE_TICKET("FREE_TICKET", "무료티켓"),
    // 유료티켓
    PRICE_TICKET("PRICE_TICKET", "유료티켓"),
}
