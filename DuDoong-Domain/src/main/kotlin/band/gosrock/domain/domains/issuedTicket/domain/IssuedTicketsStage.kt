package band.gosrock.domain.domains.issuedTicket.domain

import com.fasterxml.jackson.annotation.JsonValue

enum class IssuedTicketsStage(
    val value: String,
    @JsonValue val kr: String,
) {
    APPROVE_WAITING("APPROVE_WAITING", "승인대기"),
    AFTER_ENTRANCE("AFTER_ENTRANCE", "입장완료"),
    BEFORE_ENTRANCE("BEFORE_ENTRANCE", "관람예정"),
    ENTERING("ENTERING", "입장중"),
    CANCELED("CANCELED", "취소됨"),
    PASSED_EVENT("PASSED_EVENT", "지난공연")
}
