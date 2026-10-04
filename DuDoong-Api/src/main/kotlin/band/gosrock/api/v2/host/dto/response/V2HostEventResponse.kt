package band.gosrock.api.v2.host.dto.response

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayRule
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class V2HostEventResponse(
    val eventId: Long,
    val name: String?,
    val posterImageUrl: String?,
    @field:DateFormat
    val startAt: LocalDateTime?,
    @field:Schema(description = "공연 상태 (PREPARING / OPEN / CALCULATING / CLOSED)")
    val status: String,
    @field:Schema(description = "표시용 상태 (PREPARING / UPCOMING / ONGOING / PAST)")
    val displayStatus: V2EventDisplayStatus,
) {
    companion object {
        fun of(event: Event, now: LocalDateTime): V2HostEventResponse =
            V2HostEventResponse(
                eventId = event.id!!,
                name = event.getEventName(),
                posterImageUrl = event.eventDetail?.posterImage?.generateImageUrl(),
                startAt = event.getStartAt(),
                status = event.status.name,
                displayStatus = V2EventDisplayRule.of(event, now),
            )
    }
}
