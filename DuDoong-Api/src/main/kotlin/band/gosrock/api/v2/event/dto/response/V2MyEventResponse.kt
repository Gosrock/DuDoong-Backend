package band.gosrock.api.v2.event.dto.response

import band.gosrock.api.v2.host.dto.response.V2EventDisplayStatus
import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.event.domain.Event
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** 공연준비 홈 공연 카드 */
data class V2MyEventResponse(
    val eventId: Long,
    val hostId: Long,
    val hostName: String?,
    val name: String?,
    val posterImageUrl: String?,
    @field:DateFormat
    val startAt: LocalDateTime?,
    @field:DateFormat
    val endAt: LocalDateTime?,
    @field:Schema(description = "공연장 이름. 미입력이면 null")
    val placeName: String?,
    val placeAddress: String?,
    @field:Schema(description = "공연 상태 (PREPARING / OPEN / CALCULATING / CLOSED)")
    val status: String,
    @field:Schema(description = "표시용 상태 (PREPARING / UPCOMING / PAST)")
    val displayStatus: V2EventDisplayStatus,
    // getDDay() 는 Jackson 기본 규칙으로 "dday" 가 되므로 이름을 명시한다
    @get:JsonProperty("dDay")
    @field:Schema(description = "공연일까지 남은 일수 (D-n 의 n, 당일 0). UPCOMING 일 때만, 아니면 null")
    val dDay: Long?,
) {
    companion object {
        fun of(event: Event, hostName: String?, now: LocalDateTime): V2MyEventResponse {
            val displayStatus = V2EventDisplayStatus.of(event.status, event.getStartAt(), now)
            return V2MyEventResponse(
                eventId = event.id!!,
                hostId = event.hostId!!,
                hostName = hostName,
                name = event.getEventName(),
                posterImageUrl = event.eventDetail?.posterImage?.generateImageUrl(),
                startAt = event.getStartAt(),
                endAt = event.getEndAt(),
                placeName = event.eventPlace?.placeName,
                placeAddress = event.eventPlace?.placeAddress,
                status = event.status.name,
                displayStatus = displayStatus,
                dDay = dDayOf(displayStatus, event.getStartAt(), now),
            )
        }

        fun dDayOf(displayStatus: V2EventDisplayStatus, startAt: LocalDateTime?, now: LocalDateTime): Long? {
            if (displayStatus != V2EventDisplayStatus.UPCOMING || startAt == null) return null
            return ChronoUnit.DAYS.between(now.toLocalDate(), startAt.toLocalDate())
        }
    }
}
