package band.gosrock.api.v2.event.dto.response

import band.gosrock.api.v2.tag.dto.V2TagResponse
import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventPlace
import band.gosrock.domain.domains.event.service.v2.EventChecklist
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayRule
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayStatus
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostContactType
import band.gosrock.domain.domains.tag.domain.Tag
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** 어드민(호스팅 센터)용 공연 전체 정보 */
data class V2EventManageResponse(
    val eventId: Long,
    val hostId: Long,
    val hostName: String?,
    @field:Schema(description = "내 역할 (MASTER / MANAGER / GUEST). 멤버가 아닌 SUPER_ADMIN 이면 null")
    val myRole: String?,
    val posterImageKey: String?,
    val posterImageUrl: String?,
    val name: String?,
    @field:DateFormat
    val startAt: LocalDateTime?,
    @field:DateFormat
    val endAt: LocalDateTime?,
    @field:Schema(description = "진행 시간(분). v1 호환용 (endAt - startAt)")
    val runTime: Long?,
    @field:Schema(description = "공연 장소. 미입력이면 null")
    val place: V2EventPlaceResponse?,
    val contacts: List<V2EventContactResponse>,
    val hasTicket: Boolean,
    @field:Schema(description = "태그 (분류 순 → 분류 안 순서)")
    val tags: List<V2TagResponse>,
    @field:Schema(description = "공연 상태 (PREPARING / OPEN / CALCULATING / CLOSED)")
    val status: String,
    @field:Schema(description = "표시용 상태 (PREPARING / UPCOMING / ONGOING / PAST)")
    val displayStatus: V2EventDisplayStatus,
    // getDDay() 는 Jackson 기본 규칙으로 "dday" 가 되므로 이름을 명시한다
    @get:JsonProperty("dDay")
    @field:Schema(description = "공연일까지 남은 일수 (D-n 의 n, 당일 0). UPCOMING 일 때만, 아니면 null")
    val dDay: Long?,
    val checklist: V2EventChecklistResponse,
) {
    companion object {
        fun of(event: Event, host: Host, userId: Long, tags: List<Tag>, checklist: EventChecklist, now: LocalDateTime): V2EventManageResponse =
            V2EventManageResponse(
                eventId = event.id!!,
                hostId = host.id!!,
                hostName = host.profile?.name,
                myRole = host.getActiveRoleOf(userId)?.name,
                posterImageKey = event.eventDetail?.posterImage?.imageKey,
                posterImageUrl = event.eventDetail?.posterImage?.generateImageUrl(),
                name = event.getEventName(),
                startAt = event.getStartAt(),
                endAt = event.getEndAt(),
                runTime = event.eventBasic?.runTime,
                place = V2EventPlaceResponse.of(event.eventPlace),
                contacts = event.contacts.map { V2EventContactResponse(type = it.type, value = it.value) },
                hasTicket = event.hasTicket,
                tags = tags.map { V2TagResponse.from(it) },
                status = event.status.name,
                displayStatus = V2EventDisplayRule.of(event, now),
                dDay = V2EventDisplayRule.dDayOf(event, now),
                checklist = V2EventChecklistResponse.from(checklist),
            )
    }
}

data class V2EventPlaceResponse(
    val name: String?,
    val address: String?,
    @field:Schema(description = "상세주소 (없으면 null, #740)")
    val detailAddress: String?,
    val latitude: Double?,
    val longitude: Double?,
) {
    companion object {
        /** 이름·주소가 모두 없으면(미입력) null. E-3·P-3·H-14 공통 */
        fun of(place: EventPlace?): V2EventPlaceResponse? =
            place?.takeIf { it.placeName != null || it.placeAddress != null }?.let {
                V2EventPlaceResponse(name = it.placeName, address = it.placeAddress, detailAddress = it.placeDetailAddress, latitude = it.latitude, longitude = it.longitude)
            }
    }
}

data class V2EventContactResponse(
    val type: HostContactType,
    val value: String,
)
