package band.gosrock.api.v2.host.dto.response

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
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
    @field:Schema(description = "표시용 상태 (PREPARING / UPCOMING / PAST)")
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
                displayStatus = V2EventDisplayStatus.of(event.status, event.getStartAt(), now),
            )
    }
}

/** 호스트 홈 공연 배지용 상태 */
enum class V2EventDisplayStatus {
    PREPARING,
    UPCOMING,
    PAST;

    companion object {
        /** OPEN 이고 시작 전이면 UPCOMING, 시작이 지난 OPEN 과 CALCULATING / CLOSED 는 PAST */
        fun of(status: EventStatus, startAt: LocalDateTime?, now: LocalDateTime): V2EventDisplayStatus =
            when (status) {
                EventStatus.PREPARING -> PREPARING
                EventStatus.OPEN -> if (startAt == null || startAt.isAfter(now)) UPCOMING else PAST
                EventStatus.CALCULATING, EventStatus.CLOSED, EventStatus.DELETED -> PAST
            }
    }
}
