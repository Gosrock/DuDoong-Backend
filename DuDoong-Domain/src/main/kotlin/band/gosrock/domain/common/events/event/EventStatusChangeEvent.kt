package band.gosrock.domain.common.events.event

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus

/** 호스트 공연 상태 변경 (v1·v2, `Event.prepare/open/calculate/close`). [status] = 바뀐 상태 (#734 선물 연쇄용) */
data class EventStatusChangeEvent(
    val hostId: Long,
    val eventId: Long,
    val status: EventStatus? = null,
) : DomainEvent() {
    companion object {
        @JvmStatic
        fun of(event: Event): EventStatusChangeEvent =
            EventStatusChangeEvent(
                hostId = event.hostId ?: 0L,
                eventId = event.id ?: 0L,
                status = event.status,
            )
    }
}
