package band.gosrock.api.v2.host.dto.response

import band.gosrock.api.v2.event.dto.response.V2EventPlaceResponse
import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayRule
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayStatus
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class V2HostEventResponse(
    val eventId: Long,
    val name: String?,
    val posterImageUrl: String?,
    @field:DateFormat
    val startAt: LocalDateTime?,
    @field:Schema(description = "공연 장소 (공연 상세 P-3·E-3 과 같은 형태). 미입력이면 null")
    val place: V2EventPlaceResponse?,
    @field:Schema(description = "공연 상태 (PREPARING / OPEN / CALCULATING / CLOSED)")
    val status: String,
    @field:Schema(description = "표시용 상태 (PREPARING / UPCOMING / ONGOING / PAST)")
    val displayStatus: V2EventDisplayStatus,
    // getDDay() 는 Jackson 기본 규칙으로 "dday" 가 되므로 이름을 명시한다
    @get:JsonProperty("dDay")
    @field:Schema(description = "공연일까지 남은 일수 (D-n 의 n, 당일 0). UPCOMING 일 때만, 아니면 null")
    val dDay: Long?,
) {
    companion object {
        fun of(event: Event, now: LocalDateTime): V2HostEventResponse =
            V2HostEventResponse(
                eventId = event.id!!,
                name = event.getEventName(),
                posterImageUrl = event.eventDetail?.posterImage?.generateImageUrl(),
                startAt = event.getStartAt(),
                place = V2EventPlaceResponse.of(event.eventPlace),
                status = event.status.name,
                displayStatus = V2EventDisplayRule.of(event, now),
                dDay = V2EventDisplayRule.dDayOf(event, now),
            )
    }
}
