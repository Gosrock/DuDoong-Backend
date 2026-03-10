package band.gosrock.domain.domains.ticket_item.domain

import com.fasterxml.jackson.annotation.JsonValue

enum class TicketType(
    val value: String,
    @JsonValue val kr: String,
) {
    // 선착순
    FIRST_COME_FIRST_SERVED("FIRST_COME_FIRST_SERVED", "선착순"),
    // 승인
    APPROVAL("APPROVAL", "승인");

    /** 선착순 방식인지 반환하는 메서드 */
    fun isFCFS(): Boolean = this == FIRST_COME_FIRST_SERVED
}
