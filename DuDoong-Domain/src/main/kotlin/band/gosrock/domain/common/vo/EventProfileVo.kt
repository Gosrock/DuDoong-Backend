package band.gosrock.domain.common.vo

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import java.time.LocalDateTime

data class EventProfileVo(
    val eventId: Long? = null,
    val posterImage: ImageVo? = null,
    val name: String? = null,
    @DateFormat val startAt: LocalDateTime? = null,
    @DateFormat val endAt: LocalDateTime? = null,
    val runTime: Long? = null,
    val placeName: String? = null,
    val status: EventStatus? = null,
) {
    companion object {
        @JvmStatic
        fun from(event: Event): EventProfileVo {
            val eventBasicVo = event.toEventBasicVo()
            val eventPlaceVo = event.toEventPlaceVo()
            val eventDetailVo = event.toEventDetailVo()
            return EventProfileVo(
                eventId = event.id,
                posterImage = eventDetailVo.posterImage,
                name = eventBasicVo.name,
                startAt = eventBasicVo.startAt,
                endAt = event.getEndAt(),
                runTime = eventBasicVo.runTime,
                placeName = eventPlaceVo.placeName,
                status = event.status,
            )
        }
    }
}
