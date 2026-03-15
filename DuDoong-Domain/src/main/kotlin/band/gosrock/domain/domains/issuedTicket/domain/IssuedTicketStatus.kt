package band.gosrock.domain.domains.issuedTicket.domain

import com.fasterxml.jackson.annotation.JsonValue

enum class IssuedTicketStatus(
    val value: String,
    @JsonValue val kr: String,
) {
    ENTRANCE_COMPLETED("ENTRANCE_COMPLETED", "입장 완료"),
    ENTRANCE_INCOMPLETE("ENTRANCE_INCOMPLETE", "입장 전"),
    CANCELED("CANCELED", "취소 티켓");

    fun isCanceled(): Boolean = this == CANCELED

    fun isBeforeEntrance(): Boolean = this == ENTRANCE_INCOMPLETE

    fun isAfterEntrance(): Boolean = this == ENTRANCE_COMPLETED

    fun `is`(issuedTicket: IssuedTicket): Boolean =
        issuedTicket.issuedTicketStatus == ENTRANCE_INCOMPLETE
}
