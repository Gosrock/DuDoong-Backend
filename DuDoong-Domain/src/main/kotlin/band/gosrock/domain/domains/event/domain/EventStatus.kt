package band.gosrock.domain.domains.event.domain

import band.gosrock.common.annotation.EnumClass
import com.fasterxml.jackson.annotation.JsonValue

@EnumClass
enum class EventStatus(
    val statusName: String,
    @JsonValue val value: String,
) {
    PREPARING("PREPARING", "준비중"),
    OPEN("OPEN", "진행중"),
    CALCULATING("CALCULATING", "정산중"),
    CLOSED("CLOSED", "지난공연"),
    DELETED("DELETED", "삭제된공연");

    fun canTransitionTo(newStatus: EventStatus): Boolean =
        when (this) {
            PREPARING -> newStatus in setOf(OPEN, DELETED)
            OPEN -> newStatus in setOf(CALCULATING, DELETED)
            CALCULATING -> newStatus == CLOSED
            CLOSED, DELETED -> false
        }
}
