package band.gosrock.domain.domains.issuedTicket.domain

import com.fasterxml.jackson.annotation.JsonValue

enum class IssuedTicketCancelReason(
    val value: String,
    @JsonValue val kr: String,
) {
    REFUND("REFUND", "사용자에 의한 환불")
}
