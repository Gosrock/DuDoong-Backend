package band.gosrock.domain.common.vo

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import com.fasterxml.jackson.annotation.JsonUnwrapped
import java.time.LocalDateTime

/*
이벤트 정보 VO (공용)
 */
data class EventInfoVo(
    /** 이벤트 이름 */
    val eventName: String? = null,
    /** 이벤트 디테일 */
    @JsonUnwrapped val eventDetailVo: EventDetailVo? = null,
    /** 이벤트 시작 시간 */
    @DateFormat val startAt: LocalDateTime? = null,
    /** 이벤트 종료 시간 */
    @DateFormat val endAt: LocalDateTime? = null,
    /** 공연 장소 */
    @JsonUnwrapped val eventPlace: EventPlaceVo? = null,
    /** 공연 상태 */
    val eventStatus: EventStatus? = null,
) {
    companion object {
        @JvmStatic
        fun from(event: Event): EventInfoVo =
            EventInfoVo(
                eventName = EventBasicVo.from(event).name,
                eventDetailVo = EventDetailVo.from(event),
                startAt = event.getStartAt(),
                endAt = event.getEndAt(),
                eventPlace = EventPlaceVo.from(event),
                eventStatus = event.status,
            )
    }
}
