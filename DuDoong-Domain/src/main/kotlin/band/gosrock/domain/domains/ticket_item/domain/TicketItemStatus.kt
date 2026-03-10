package band.gosrock.domain.domains.ticket_item.domain

import com.fasterxml.jackson.annotation.JsonValue

enum class TicketItemStatus(
    val value: String,
    @JsonValue val kr: String,
) {
    // 유효
    VALID("VALID", "유효"),
    // 삭제됨
    DELETED("DELETED", "삭제됨"),
}
