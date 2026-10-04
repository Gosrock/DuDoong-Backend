package band.gosrock.api.v2.event.dto.response

import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayRule
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** 등록(E-8) / 삭제(E-9) 결과 */
data class V2EventStatusResponse(
    val eventId: Long,
    @field:Schema(description = "공연 상태 (PREPARING / OPEN / CALCULATING / CLOSED / DELETED)")
    val status: String,
    @field:Schema(description = "표시용 상태 (PREPARING / UPCOMING / ONGOING / PAST)")
    val displayStatus: V2EventDisplayStatus,
) {
    companion object {
        fun of(event: Event, now: LocalDateTime): V2EventStatusResponse =
            V2EventStatusResponse(
                eventId = event.id!!,
                status = event.status.name,
                displayStatus = V2EventDisplayRule.of(event, now),
            )
    }
}
