package band.gosrock.api.event.model.dto.response

import band.gosrock.domain.common.vo.EventBasicVo
import band.gosrock.domain.common.vo.EventDetailVo
import band.gosrock.domain.common.vo.EventPlaceVo
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import com.fasterxml.jackson.annotation.JsonUnwrapped

data class EventResponse(
    val eventId: Long?,
    val hostId: Long?,
    val status: EventStatus?,
    @JsonUnwrapped val eventBasic: EventBasicVo?,
    @JsonUnwrapped val eventDetail: EventDetailVo?,
    val place: EventPlaceVo?
) {
    companion object {
        @JvmStatic
        fun of(event: Event): EventResponse {
            return EventResponse(
                eventId = event.id,
                hostId = event.hostId,
                eventBasic = EventBasicVo.from(event),
                eventDetail = EventDetailVo.from(event),
                place = EventPlaceVo.from(event),
                status = event.status
            )
        }
    }
}
